package tecgraf.openbus.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UserException;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;

import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.v2_1.BusObjectKey;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.credential.SignedDataHelper;
import tecgraf.openbus.core.v2_1.data_export.CurrentVersion;
import tecgraf.openbus.core.v2_1.data_export.ExportedCallChain;
import tecgraf.openbus.core.v2_1.data_export.ExportedCallChainHelper;
import tecgraf.openbus.core.v2_1.data_export.ExportedSharedAuth;
import tecgraf.openbus.core.v2_1.data_export.ExportedSharedAuthHelper;
import tecgraf.openbus.core.v2_1.data_export.ExportedVersion;
import tecgraf.openbus.core.v2_1.data_export.ExportedVersionSeqHelper;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.InvalidEncodedStream;
import tecgraf.openbus.exception.InvalidPropertyValue;
import tecgraf.openbus.exception.OpenBusInternalException;

/**
 * Implementação do multiplexador de conexão.
 * 
 * @author Tecgraf
 */
final class OpenBusContextImpl extends LocalObject implements OpenBusContext {

  /** Logger. */
  private static final Logger logger = Logger
    .getLogger(OpenBusContextImpl.class.getName());

  /** Tamanho da tag de identificação de dado exportado. */
  private static final int MAGIC_TAG_SIZE = 4;
  /** Tag de cadeia */
  private static final byte[] MTAG_CALLCHAIN =
    new byte[] { 'B', 'U', 'S', 0x01 };
  /** Tag de compartilhamento de autenticação */
  private static final byte[] MTAG_SHAREDAUTH = new byte[] { 'B', 'U', 'S',
      0x02 };

  /** Identificador do slot de conexao corrente */
  private final int CURRENT_CONNECTION_SLOT_ID;
  /** Identificador do slot de interceptação ignorada */
  private final int IGNORE_THREAD_SLOT_ID;
  /** Identificador de slot de flag sobre callback OnInvalidLogin */
  private int SKIP_INVLOGIN_SLOT_ID;

  /** Mapa de conexão por Requester */
  private Map<Integer, Connection> connectedById;
  /** Conexão padrão */
  private Connection defaultConn;
  /** Callback a ser disparada caso o login se encontre inválido */
  private CallDispatchCallback dispatchCallback;

  /** Referência para o ORB ao qual pertence */
  private ORB orb;

  /** Lock para operações sobre conexões */
  private final ReentrantReadWriteLock rwlock =
    new ReentrantReadWriteLock(true);
  /** Lock de leitura para operações sobre conexões */
  private final ReadLock readLock = rwlock.readLock();
  /** Lock de escrita para operações sobre conexões */
  private final WriteLock writeLock = rwlock.writeLock();

  /**
   * Construtor.
   * 
   * @param currentConnectionSlotId identificador do slot da conexão corrente
   * @param ignoreThreadSlotId identificador do slot de interceptação ignorada.
   * @param invLoginSlotId identificador de slot de invalid login
   */
  public OpenBusContextImpl(int currentConnectionSlotId,
    int ignoreThreadSlotId, int invLoginSlotId) {
    this.connectedById =
      Collections.synchronizedMap(new HashMap<Integer, Connection>());
    this.CURRENT_CONNECTION_SLOT_ID = currentConnectionSlotId;
    this.IGNORE_THREAD_SLOT_ID = ignoreThreadSlotId;
    this.SKIP_INVLOGIN_SLOT_ID = invLoginSlotId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ORB orb() {
    return this.orb;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection connectByAddress(String host, int port) {
    Connection conn;
    try {
      conn = connectByAddress(host, port, new Properties());
    }
    catch (InvalidPropertyValue e) {
      throw new OpenBusInternalException(
        "BUG: Este erro nunca deveria ocorrer.", e);
    }
    return conn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection connectByAddress(String host, int port, Properties props)
    throws InvalidPropertyValue {
    if ((host == null) || (host.isEmpty()) || (port < 0)) {
      throw new IllegalArgumentException(
        "Os parametros host e/ou port não são validos");
    }
    try {
      ignoreThread();
      String str =
        String.format("corbaloc::1.0@%s:%d/%s", host, port, BusObjectKey.value);
      org.omg.CORBA.Object obj = orb.string_to_object(str);
      BusInfo info = new BusInfo(obj);
      return new ConnectionImpl(info, this, orb, props);
    }
    finally {
      unignoreThread();
    }
  }

  /**
   * {@inheritDoc}
   */
  public Connection connectByReference(org.omg.CORBA.Object reference) {
    Connection conn;
    try {
      conn = connectByReference(reference, new Properties());
    }
    catch (InvalidPropertyValue e) {
      throw new OpenBusInternalException(
        "BUG: Este erro nunca deveria ocorrer.", e);
    }
    return conn;
  }

  /**
   * {@inheritDoc}
   */
  public Connection connectByReference(org.omg.CORBA.Object reference,
    Properties props) throws InvalidPropertyValue {
    if (reference == null) {
      throw new IllegalArgumentException(
        "Referência inválida para o barramento.");
    }
    BusInfo bus = new BusInfo(reference);
    return new ConnectionImpl(bus, this, orb, props);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public Connection createConnection(String host, int port) {
    return connectByAddress(host, port);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public Connection createConnection(String host, int port, Properties props)
    throws InvalidPropertyValue {
    return connectByAddress(host, port, props);
  }

  /**
   * Recupera a chave do slot de identificação da conexão corrente.
   * 
   * @return a chave do slot
   */
  int getCurrentConnectionSlotId() {
    return this.CURRENT_CONNECTION_SLOT_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection setDefaultConnection(Connection conn) {
    Connection old;
    this.writeLock.lock();
    try {
      old = this.defaultConn;
      this.defaultConn = conn;
    }
    finally {
      this.writeLock.unlock();
    }
    return old;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getDefaultConnection() {
    this.readLock.lock();
    try {
      return this.defaultConn;
    }
    finally {
      this.readLock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection setCurrentConnection(Connection conn) {
    int id = ORBUtils.getMediator(orb).getUniqueId();
    Any any = this.orb.create_any();
    if (conn != null) {
      any.insert_long(id);
    }
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(CURRENT_CONNECTION_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return setConnectionById(id, conn);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getCurrentConnection() {
    Connection connection = null;
    Current current = ORBUtils.getPICurrent(orb);
    Any any;
    try {
      any = current.get_slot(CURRENT_CONNECTION_SLOT_ID);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

    if (any.type().kind().value() != TCKind._tk_null) {
      int id = any.extract_long();
      connection = this.connectedById.get(id);
    }

    if (connection == null) {
      connection = getDefaultConnection();
    }
    return connection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain getCallerChain() {
    Current current = ORBUtils.getPICurrent(orb);
    ORBMediator mediator = ORBUtils.getMediator(orb);
    try {
      Any busany = current.get_slot(mediator.getBusSlotId());
      Any signedany = current.get_slot(mediator.getSignedChainSlotId());
      if ((busany.type().kind().value() != TCKind._tk_null)
        && (signedany.type().kind().value() != TCKind._tk_null)) {
        String busId = busany.extract_string();
        SignedData signedChain = SignedDataHelper.extract(signedany);
        Any anyChain =
          mediator.getCodec().decode_value(signedChain.encoded,
            CallChainHelper.type());
        CallChain callChain = CallChainHelper.extract(anyChain);
        return new CallerChainImpl(callChain, signedChain);
      }
      return null;
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao recuperar a cadeia.";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joinChain() throws OpenBusInternalException {
    joinChain(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joinChain(CallerChain chain) {
    chain = (chain != null) ? chain : getCallerChain();
    if (chain == null) {
      return;
    }
    try {
      Current current = ORBUtils.getPICurrent(orb);
      ORBMediator mediator = ORBUtils.getMediator(orb);
      SignedData signedChain = ((CallerChainImpl) chain).signedCallChain();
      Any any = this.orb.create_any();
      SignedDataHelper.insert(any, signedChain);
      current.set_slot(mediator.getJoinedChainSlotId(), any);
      Any busAny = this.orb.create_any();
      busAny.insert_string(chain.busid());
      current.set_slot(mediator.getJoinedBusSlotId(), busAny);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void exitChain() {
    try {
      Current current = ORBUtils.getPICurrent(orb);
      ORBMediator mediator = ORBUtils.getMediator(orb);
      Any any = this.orb.create_any();
      current.set_slot(mediator.getJoinedChainSlotId(), any);
      current.set_slot(mediator.getJoinedBusSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain getJoinedChain() {
    try {
      Current current = ORBUtils.getPICurrent(orb);
      ORBMediator mediator = ORBUtils.getMediator(orb);
      Any anybus = current.get_slot(mediator.getJoinedBusSlotId());
      Any anyschain = current.get_slot(mediator.getJoinedChainSlotId());

      if ((anybus.type().kind().value() != TCKind._tk_null)
        && (anyschain.type().kind().value() != TCKind._tk_null)) {
        String busId = anybus.extract_string();
        SignedData signedChain = SignedDataHelper.extract(anyschain);
        Any chain =
          mediator.getCodec().decode_value(signedChain.encoded,
            CallChainHelper.type());
        CallChain callChain = CallChainHelper.extract(chain);
        return new CallerChainImpl(callChain, signedChain);
      }
      return null;
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao recuperar a cadeia.";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain makeChainFor(String loginId) throws InvalidLogins,
    ServiceFailure {
    ConnectionImpl conn = (ConnectionImpl) getCurrentConnection();
    String busid = conn.busid();
    SignedData signed = conn.access().signChainFor(loginId);
    try {
      ORBMediator mediator = ORBUtils.getMediator(orb);
      Any anyChain =
        mediator.getCodec()
          .decode_value(signed.encoded, CallChainHelper.type());
      CallChain callChain = CallChainHelper.extract(anyChain);
      return new CallerChainImpl(callChain, signed);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao criar uma nova cadeia.";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  };

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] encodeChain(CallerChain chain) {
    ORBMediator mediator = ORBUtils.getMediator(orb);
    Codec codec = mediator.getCodec();
    try {
      Any anyChain = orb.create_any();
      CallerChainImpl chainImpl = (CallerChainImpl) chain;
      ExportedCallChainHelper.insert(anyChain, new ExportedCallChain(chain
        .busid(), chainImpl.signedCallChain()));
      byte[] encodedChain = codec.encode_value(anyChain);
      ExportedVersion[] exports =
        new ExportedVersion[] { new ExportedVersion(CurrentVersion.value,
          encodedChain) };
      return encodeExportedVersions(exports, MTAG_CALLCHAIN);
    }
    catch (InvalidTypeForEncoding e) {
      String message =
        "Falha inesperada ao codificar uma cadeia para exportação";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain decodeChain(byte[] encoded) throws InvalidEncodedStream {
    ORBMediator mediator = ORBUtils.getMediator(orb);
    Codec codec = mediator.getCodec();
    try {
      ExportedVersion[] exports =
        decodeExportedVersions(encoded, MTAG_CALLCHAIN);
      for (ExportedVersion export : exports) {
        if (export.version == CurrentVersion.value) {
          Any exported =
            codec.decode_value(export.encoded, ExportedCallChainHelper.type());
          ExportedCallChain importing =
            ExportedCallChainHelper.extract(exported);
          Any anyChain =
            codec.decode_value(importing.signedChain.encoded, CallChainHelper
              .type());
          CallChain chain = CallChainHelper.extract(anyChain);
          return new CallerChainImpl(importing.bus, chain.target, chain.caller,
            chain.originators, importing.signedChain);
        }
      }
    }
    catch (UserException e) {
      String message = "Falha inesperada ao decodificar uma cadeia exportada.";
      logger.log(Level.SEVERE, message, e);
      throw new InvalidEncodedStream(message, e);
    }
    throw new InvalidEncodedStream("Versão de cadeia incompatível");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] encodeSharedAuth(SharedAuthSecret secret) {
    ORBMediator mediator = ORBUtils.getMediator(orb);
    Codec codec = mediator.getCodec();
    try {
      Any anySecret = orb.create_any();
      SharedAuthSecretImpl secretImpl = (SharedAuthSecretImpl) secret;
      ExportedSharedAuthHelper.insert(anySecret, new ExportedSharedAuth(
        secretImpl.busid(), secretImpl.attempt(), secretImpl.secret()));
      byte[] encodedSecret = codec.encode_value(anySecret);
      ExportedVersion[] exports =
        new ExportedVersion[] { new ExportedVersion(CurrentVersion.value,
          encodedSecret) };
      return encodeExportedVersions(exports, MTAG_SHAREDAUTH);
    }
    catch (InvalidTypeForEncoding e) {
      String message =
        "Falha inesperada ao codificar um segredo para exportação";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  };

  /**
   * {@inheritDoc}
   */
  @Override
  public SharedAuthSecret decodeSharedAuth(byte[] encoded)
    throws InvalidEncodedStream {
    ORBMediator mediator = ORBUtils.getMediator(orb);
    Codec codec = mediator.getCodec();
    try {
      ExportedVersion[] exports =
        decodeExportedVersions(encoded, MTAG_SHAREDAUTH);
      for (ExportedVersion export : exports) {
        if (export.version == CurrentVersion.value) {
          Any exported =
            codec.decode_value(export.encoded, ExportedSharedAuthHelper.type());
          ExportedSharedAuth secret =
            ExportedSharedAuthHelper.extract(exported);
          return new SharedAuthSecretImpl(secret.bus, secret.attempt,
            secret.secret, this);
        }
      }
    }
    catch (UserException e) {
      String message = "Falha inesperada ao decodificar um segredo exportado.";
      logger.log(Level.SEVERE, message, e);
      throw new InvalidEncodedStream(message, e);
    }
    throw new InvalidEncodedStream("Versão de segredo incompatível");

  };

  /**
   * Codifica um conjunto de dados no formato padrão de exportação.
   * 
   * @param exports dados a serem codificados
   * @param tag a tag de associação da semântica do dado
   * @return o dado condificado.
   * @throws InvalidTypeForEncoding
   */
  private byte[] encodeExportedVersions(ExportedVersion[] exports, byte[] tag)
    throws InvalidTypeForEncoding {
    ORBMediator mediator = ORBUtils.getMediator(orb);
    Codec codec = mediator.getCodec();
    Any any = orb.create_any();
    ExportedVersionSeqHelper.insert(any, exports);
    byte[] encodedExport = codec.encode_value(any);
    byte[] fullEnconding = new byte[encodedExport.length + MAGIC_TAG_SIZE];
    System.arraycopy(tag, 0, fullEnconding, 0, MAGIC_TAG_SIZE);
    System.arraycopy(encodedExport, 0, fullEnconding, MAGIC_TAG_SIZE,
      encodedExport.length);
    return fullEnconding;
  }

  /**
   * Decodifica um conjunto de dados no formato padrão de exportação.
   * 
   * @param encoded o dado a ser decodificado
   * @param magictag a semântica esperada do dado
   * @return o conjunto de dados decodificado.
   * @throws InvalidEncodedStream
   * @throws TypeMismatch
   * @throws FormatMismatch
   */
  private ExportedVersion[] decodeExportedVersions(byte[] encoded,
    byte[] magictag) throws InvalidEncodedStream, FormatMismatch, TypeMismatch {
    if (encoded.length > MAGIC_TAG_SIZE) {
      byte[] tag = new byte[MAGIC_TAG_SIZE];
      byte[] encodedExport = new byte[encoded.length - MAGIC_TAG_SIZE];
      System.arraycopy(encoded, 0, tag, 0, MAGIC_TAG_SIZE);
      System.arraycopy(encoded, MAGIC_TAG_SIZE, encodedExport, 0,
        encodedExport.length);
      if (Arrays.equals(magictag, tag)) {
        ORBMediator mediator = ORBUtils.getMediator(orb);
        Codec codec = mediator.getCodec();
        Any anyexports =
          codec.decode_value(encodedExport, ExportedVersionSeqHelper.type());
        return ExportedVersionSeqHelper.extract(anyexports);
      }
    }
    String errormsg =
      "Stream de bytes não corresponde ao tipo de dado esperado.";
    throw new InvalidEncodedStream(errormsg);
  }

  /**
   * Recupera a conexão associada ao identificador.
   * 
   * @param id o identificador
   * @return a conexão em uso.
   */
  Connection getConnectionById(int id) {
    return this.connectedById.get(id);
  }

  /**
   * Configura a conexão em uso para o identificador especificado..
   * 
   * @param id identificador.
   * @param conn a conexão em uso.
   * @return a antiga conexão configurada.
   */
  Connection setConnectionById(int id, Connection conn) {
    synchronized (this.connectedById) {
      Connection old = this.connectedById.remove(id);
      if (conn != null) {
        this.connectedById.put(id, conn);
      }
      return old;
    }
  }

  /**
   * Configura o ORB que o multiplexador esta associado.
   * 
   * @param orb o ORB.
   */
  void setORB(ORB orb) {
    this.orb = orb;
  }

  /**
   * Sinaliza que as requisições realizadas através desta thread não devem ser
   * interceptadas.
   */
  void ignoreThread() {
    Any any = this.orb.create_any();
    any.insert_boolean(true);
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(IGNORE_THREAD_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message =
        "Falha inesperada ao acessar o slot de interceptação ignorada";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Siznaliza que as requisições realizadas através desta thread devem voltar a
   * ser interceptadas.
   */
  void unignoreThread() {
    Any any = this.orb.create_any();
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(IGNORE_THREAD_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message =
        "Falha inesperada ao acessar o slot de interceptação ignorada";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Recupera o identificador de slot que indica se deve ignorar a
   * intereceptação ou não.
   * 
   * @return o identificador
   */
  int getIgnoreThreadSlotId() {
    return IGNORE_THREAD_SLOT_ID;
  }

  /**
   * Sinaliza que as requisições realizadas através deste Current não devem
   * tentar o relogin.
   */
  void ignoreInvLogin() {
    Any any = this.orb.create_any();
    any.insert_boolean(true);
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(SKIP_INVLOGIN_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot de callback";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Siznaliza que as requisições realizadas através deste Current devem voltar
   * a tentar o relogin.
   */
  void unignoreInvLogin() {
    Any any = this.orb.create_any();
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(SKIP_INVLOGIN_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot de InvalidLogin";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Recupera o identificador de slot que indica se deve ignorar a tentativa de
   * relogin ou não.
   * 
   * @return o identificador
   */
  int getSkipInvalidLoginSlotId() {
    return SKIP_INVLOGIN_SLOT_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCallDispatch(CallDispatchCallback callback) {
    this.dispatchCallback = callback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallDispatchCallback onCallDispatch() {
    return this.dispatchCallback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginRegistry getLoginRegistry() {
    ConnectionImpl conn = (ConnectionImpl) getCurrentConnection();
    if (conn == null || conn.login() == null) {
      throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus.COMPLETED_NO);
    }
    return conn.logins();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OfferRegistry getOfferRegistry() {
    ConnectionImpl conn = (ConnectionImpl) getCurrentConnection();
    if (conn == null || conn.login() == null) {
      throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus.COMPLETED_NO);
    }
    return conn.offers();
  }

}
