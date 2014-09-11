package tecgraf.openbus.core;

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
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.credential.CredentialContextId;
import tecgraf.openbus.core.v2_1.credential.ExportedCallChain;
import tecgraf.openbus.core.v2_1.credential.ExportedCallChainHelper;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.credential.SignedDataHelper;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.InvalidChainStream;
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

  /**
   * Constante que define o tamanho em bytes da codificação do identificador da
   * versão da cadeia de chamadas exportada.
   */
  private static final int CHAIN_HEADER_SIZE = 8;

  /** Identificador do slot de conexao corrente */
  private final int CURRENT_CONNECTION_SLOT_ID;
  /** Identificador do slot de interceptação ignorada */
  private final int IGNORE_THREAD_SLOT_ID;
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
   */
  public OpenBusContextImpl(int currentConnectionSlotId, int ignoreThreadSlotId) {
    this.connectedById =
      Collections.synchronizedMap(new HashMap<Integer, Connection>());
    this.CURRENT_CONNECTION_SLOT_ID = currentConnectionSlotId;
    this.IGNORE_THREAD_SLOT_ID = ignoreThreadSlotId;
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
  @Deprecated
  public Connection createConnection(String host, int port) {
    ConnectionImpl conn;
    try {
      conn = new ConnectionImpl(host, port, this, orb);
    }
    catch (InvalidPropertyValue e) {
      // Nunca deveria acontecer
      throw new OpenBusInternalException(
        "BUG: Este erro nunca deveria ocorrer.", e);
    }
    return conn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public Connection createConnection(String host, int port, Properties props)
    throws InvalidPropertyValue {
    return new ConnectionImpl(host, port, this, orb, props);
  }

  /**
   * Recupera a chave do slot de identificação da conexão corrente.
   * 
   * @return a chave do slot.s
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
    String busId;
    CallChain callChain;
    SignedData signedChain;
    try {
      Any any = current.get_slot(mediator.getBusSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      busId = any.extract_string();
      any = current.get_slot(mediator.getSignedChainSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      signedChain = SignedDataHelper.extract(any);
      Any anyChain =
        mediator.getCodec().decode_value(signedChain.encoded,
          CallChainHelper.type());
      callChain = CallChainHelper.extract(anyChain);
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
    return new CallerChainImpl(busId, callChain.target, callChain.caller,
      callChain.originators, signedChain);
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
      Any targetAny = this.orb.create_any();
      targetAny.insert_string(chain.target());
      current.set_slot(mediator.getJoinedChainTargetSlotId(), targetAny);
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
      current.set_slot(mediator.getJoinedChainTargetSlotId(), any);
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

      Any any = current.get_slot(mediator.getJoinedBusSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      String busId = any.extract_string();

      any = current.get_slot(mediator.getJoinedChainSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      SignedData signedChain = SignedDataHelper.extract(any);
      Any anyChain =
        mediator.getCodec().decode_value(signedChain.encoded,
          CallChainHelper.type());
      CallChain callChain = CallChainHelper.extract(anyChain);
      any = current.get_slot(mediator.getJoinedChainTargetSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      String target = any.extract_string();
      return new CallerChainImpl(busId, target, callChain.caller,
        callChain.originators, signedChain);
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
    SignedData signedChain = conn.access().signChainFor(loginId);
    try {
      ORBMediator mediator = ORBUtils.getMediator(orb);
      Any anyChain =
        mediator.getCodec().decode_value(signedChain.encoded,
          CallChainHelper.type());
      CallChain callChain = CallChainHelper.extract(anyChain);
      return new CallerChainImpl(busid, callChain.target, callChain.caller,
        callChain.originators, signedChain);
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
    Any anyChain = orb.create_any();
    CallerChainImpl chainImpl = (CallerChainImpl) chain;
    ExportedCallChainHelper.insert(anyChain, new ExportedCallChain(chain
      .busid(), chainImpl.signedCallChain()));
    byte[] encodedChain;
    Any anyId = orb.create_any();
    anyId.insert_long(CredentialContextId.value);
    byte[] encodedId;
    try {
      encodedChain = codec.encode_value(anyChain);
      encodedId = codec.encode_value(anyId);
    }
    catch (InvalidTypeForEncoding e) {
      String message =
        "Falha inesperada ao codificar uma cadeia para exportação";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    byte[] fullEnconding = new byte[encodedChain.length + encodedId.length];
    System.arraycopy(encodedId, 0, fullEnconding, 0, encodedId.length);
    System.arraycopy(encodedChain, 0, fullEnconding, encodedId.length,
      encodedChain.length);
    return fullEnconding;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain decodeChain(byte[] encoded) throws InvalidChainStream {
    if (encoded.length <= CHAIN_HEADER_SIZE) {
      String msg = "Stream de bytes não corresponde a uma cadeia de chamadas.";
      throw new InvalidChainStream(msg);
    }
    ORBMediator mediator = ORBUtils.getMediator(orb);
    Codec codec = mediator.getCodec();
    ExportedCallChain importedChain;
    CallChain callChain;

    byte[] encodedId = new byte[CHAIN_HEADER_SIZE];
    byte[] encodedChain = new byte[encoded.length - CHAIN_HEADER_SIZE];
    System.arraycopy(encoded, 0, encodedId, 0, encodedId.length);
    System.arraycopy(encoded, encodedId.length, encodedChain, 0,
      encodedChain.length);
    try {
      Any anyId =
        codec.decode_value(encodedId, orb.get_primitive_tc(TCKind.tk_long));
      int id = anyId.extract_long();
      if (CredentialContextId.value != id) {
        String msg =
          String
            .format(
              "Formato da cadeia é de versão incompatível.\nFormato recebido = %i\nFormato suportado = %i",
              id, CredentialContextId.value);
        throw new InvalidChainStream(msg);
      }
      Any anyExportedChain =
        codec.decode_value(encodedChain, ExportedCallChainHelper.type());
      importedChain = ExportedCallChainHelper.extract(anyExportedChain);
      Any anyChain =
        mediator.getCodec().decode_value(importedChain.signedChain.encoded,
          CallChainHelper.type());
      callChain = CallChainHelper.extract(anyChain);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao decodificar uma cadeia exportada.";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return new CallerChainImpl(importedChain.bus, callChain.target,
      callChain.caller, callChain.originators, importedChain.signedChain);
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
  void ignoreCurrentThread() {
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
  void unignoreCurrentThread() {
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
   * Verifica se a requisição corrente esta configurada para ignorar a
   * interceptação ou não.
   * 
   * @param ri informação da requisição.
   * @return <code>true</code> caso a interceptação deve ser ignorada, e
   *         <code>false</code> caso contrário.
   */
  boolean isCurrentThreadIgnored(RequestInfo ri) {
    Any any;
    try {
      any = ri.get_slot(IGNORE_THREAD_SLOT_ID);
    }
    catch (InvalidSlot e) {
      String message =
        "Falha inesperada ao obter o slot de interceptação ignorada";
      throw new INTERNAL(message);
    }
    if (any.type().kind().value() != TCKind._tk_null) {
      boolean isIgnored = any.extract_boolean();
      return isIgnored;
    }
    return false;
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
