package tecgraf.openbus.core;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UserException;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.core.v2_0.BusObjectKey;
import tecgraf.openbus.core.v2_0.EncryptedBlockHolder;
import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.credential.SignedCallChainHelper;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_0.services.access_control.CertificateRegistry;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidPublicKey;
import tecgraf.openbus.core.v2_0.services.access_control.LoginAuthenticationInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginAuthenticationInfoHelper;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.BusChanged;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.InvalidBusAddress;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.InvalidPrivateKey;
import tecgraf.openbus.exception.InvalidPropertyValue;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.util.Cryptography;

/**
 * Implementação da Interface {@link Connection}
 * 
 * @author Tecgraf
 */
final class ConnectionImpl implements Connection {
  /** Identificador da conexão */
  private final String connId = UUID.randomUUID().toString();
  /** Instância do logger */
  private static final Logger logger = Logger.getLogger(ConnectionImpl.class
    .getName());
  /** Instância auxiliar para tratar de criptografia */
  private Cryptography crypto;

  /** ORB associado a esta conexão */
  private ORB orb;
  /** Gerente da conexão. */
  private ConnectionManagerImpl manager;
  /** Informações sobre o barramento ao qual a conexão pertence */
  private BusInfo bus;
  /** Informações sobre o legacy do barramento ao qual a conexão pertence */
  private LegacyInfo legacyBus;
  /** Chave pública do sdk */
  private RSAPublicKey publicKey;
  /** Chave privada do sdk */
  private RSAPrivateKey privateKey;
  /** Informação do login associado a esta conexão. */
  private InternalLogin internalLogin;
  /** Lock para operações sobre o login */
  private final ReentrantReadWriteLock rwlock =
    new ReentrantReadWriteLock(true);
  /** Lock de leitura para operações sobre o login */
  private final ReadLock readLock = rwlock.readLock();
  /** Lock de escrita para operações sobre o login */
  private final WriteLock writeLock = rwlock.writeLock();

  /** Mapa de thread para cadeia de chamada */
  private Map<Thread, CallerChain> joinedChains;
  /** Thread de renovação de login */
  private LeaseRenewer renewer;
  /** Callback a ser disparada caso o login se encontre inválido */
  private InvalidLoginCallback invalidLoginCallback;

  /* Propriedades da conexão. */
  /** Informa se o suporte legado esta ativo */
  private boolean legacy;
  /** Informa qual o modelo de preenchimento do campo delegate. */
  private String delegate;

  /**
   * Construtor.
   * 
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * @param manager Implementação do multiplexador de conexão.
   * @param orb ORB que essa conexão ira utilizar;
   * @throws InvalidBusAddress par host/porta não corresponde a um barramento
   *         acessível.
   * @throws InvalidPropertyValue Existe uma propriedade com um valor inválido.
   */
  public ConnectionImpl(String host, int port, ConnectionManagerImpl manager,
    ORB orb) throws InvalidBusAddress, InvalidPropertyValue {
    this(host, port, manager, orb, new Properties());
  }

  /**
   * Construtor.
   * 
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * @param manager Implementação do multiplexador de conexão.
   * @param orb ORB que essa conexão ira utilizar;
   * @param props Propriedades da conexão.
   * @throws InvalidBusAddress par host/porta não corresponde a um barramento
   *         acessível.
   * @throws InvalidPropertyValue Existe uma propriedade com um valor inválido.
   */
  public ConnectionImpl(String host, int port, ConnectionManagerImpl manager,
    ORB orb, Properties props) throws InvalidBusAddress, InvalidPropertyValue {
    if ((host == null) || (host.isEmpty()) || (port < 0)) {
      throw new InvalidBusAddress(
        "Os parametros host e/ou port não são validos");
    }

    this.orb = orb;
    this.manager = manager;
    this.bus = null;
    this.legacyBus = null;
    if (props == null) {
      props = new Properties();
    }
    String prop = Property.LEGACY_DISABLE.getProperty(props);
    Boolean disabled = Boolean.valueOf(prop);
    this.legacy = !disabled;
    this.delegate = Property.LEGACY_DELEGATE.getProperty(props);
    try {
      this.manager.ignoreCurrentThread();
      retrieveBusReferences(host, port);
    }
    finally {
      this.manager.unignoreCurrentThread();
    }

    try {
      this.crypto = Cryptography.getInstance();
      KeyPair keyPair = crypto.generateRSAKeyPair();
      this.publicKey = (RSAPublicKey) keyPair.getPublic();
      this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
      this.joinedChains =
        Collections.synchronizedMap(new HashMap<Thread, CallerChain>());
    }
    catch (CryptographyException e) {
      throw new OpenBusInternalException(
        "Erro inexperado na geração do par de chaves.", e);
    }
    this.internalLogin = new InternalLogin(this);
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
  public String busid() {
    return getBus().getId();
  }

  /**
   * Recupera a chave pública do barramento.
   * 
   * @return a chave pública.
   */
  RSAPublicKey getBusPublicKey() {
    return getBus().getPublicKey();
  }

  /**
   * Recupera a chave privada da conexão.
   * 
   * @return a chave privada.
   */
  RSAPrivateKey getPrivateKey() {
    return this.privateKey;
  }

  /**
   * Verifica se a conexão já está loggada, e lança a exceção
   * {@link AlreadyLoggedIn} caso positivo.
   * 
   * @throws AlreadyLoggedIn
   */
  private void checkLoggedIn() throws AlreadyLoggedIn {
    LoginInfo login = this.internalLogin.login();
    if (login != null) {
      throw new AlreadyLoggedIn();
    }
  }

  /**
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * @throws InvalidBusAddress para host/porta não aponta para um barramento.
   */
  private void retrieveBusReferences(String host, int port)
    throws InvalidBusAddress {
    String str =
      String.format("corbaloc::1.0@%s:%d/%s", host, port, BusObjectKey.value);
    org.omg.CORBA.Object obj = orb.string_to_object(str);
    boolean existent = false;
    try {
      if (obj != null && !obj._non_existent()) {
        existent = true;
      }
    }
    catch (OBJECT_NOT_EXIST e) {
      // o tratamento esta no finally
    }
    finally {
      if (!existent) {
        throw new InvalidBusAddress(
          "Não foi possível obter uma referência para o barramento.");
      }
    }
    IComponent acsComponent = null;
    if (obj._is_a(IComponentHelper.id())) {
      acsComponent = IComponentHelper.narrow(obj);
    }
    if (acsComponent == null) {
      throw new InvalidBusAddress(
        "Referência obtida não corresponde a um IComponent.");
    }
    this.bus = new BusInfo(acsComponent);

    if (this.legacy) {
      String legacyStr =
        String.format("corbaloc::1.0@%s:%d/%s", host, port, "openbus_v1_05");
      org.omg.CORBA.Object legacyObj = orb.string_to_object(legacyStr);
      existent = false;
      try {
        if (legacyObj != null && !legacyObj._non_existent()) {
          existent = true;
        }
      }
      catch (OBJECT_NOT_EXIST e) {
        // o tratamento esta no finally
      }
      finally {
        if (!existent) {
          this.legacy = false;
          return;
        }
      }
      IComponent acsLegacyComponent = null;
      if (obj._is_a(IComponentHelper.id())) {
        acsLegacyComponent = IComponentHelper.narrow(legacyObj);
      }
      if (acsLegacyComponent == null) {
        this.legacy = false;
        return;
      }
      this.legacyBus = new LegacyInfo(acsLegacyComponent);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginByPassword(String entity, byte[] password)
    throws AccessDenied, AlreadyLoggedIn, ServiceFailure, BusChanged {
    checkLoggedIn();
    LoginInfo newLogin;
    try {
      this.manager.ignoreCurrentThread();
      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(password);
      IntHolder validityHolder = new IntHolder();

      newLogin =
        getBus().getAccessControl().loginByPassword(entity,
          this.publicKey.getEncoded(), encryptedLoginAuthenticationInfo,
          validityHolder);
      localLogin(newLogin, validityHolder.value);
    }
    catch (WrongEncoding e) {
      throw new ServiceFailure(
        "Falhou a codificação com a chave pública do barramento");
    }
    catch (InvalidPublicKey e) {
      throw new OpenBusInternalException(
        "Falha no protocolo OpenBus: A chave de acesso gerada não foi aceita. Mensagem="
          + e.message);
    }
    finally {
      this.manager.unignoreCurrentThread();
    }
    logger
      .info(String
        .format(
          "Login por senha efetuado com sucesso: busid (%s) login (%s) entidade (%s)",
          busid(), newLogin.id, newLogin.entity));
  }

  /**
   * Cria a estrutura {@link LoginAuthenticationInfo} encriptada com a pública
   * do barramento.
   * 
   * @param data Dado para autenticação.
   * @return O {@link LoginAuthenticationInfo} encriptado.
   * @throws OpenBusInternalException
   */
  private byte[] generateEncryptedLoginAuthenticationInfo(byte[] data) {
    try {
      byte[] publicKeyHash = crypto.generateHash(this.publicKey.getEncoded());

      LoginAuthenticationInfo authenticationInfo =
        new LoginAuthenticationInfo(publicKeyHash, data);
      Any authenticationInfoAny = this.orb.create_any();
      LoginAuthenticationInfoHelper.insert(authenticationInfoAny,
        authenticationInfo);
      ORBMediator mediator = ORBUtils.getMediator(orb);
      byte[] encodedLoginAuthenticationInfo =
        mediator.getCodec().encode_value(authenticationInfoAny);
      return crypto.encrypt(encodedLoginAuthenticationInfo, getBus()
        .getPublicKey());
    }
    catch (InvalidTypeForEncoding e) {
      throw new OpenBusInternalException(
        "Falha inesperada ao codificar as informações de autenticação", e);
    }
    catch (CryptographyException e) {
      throw new OpenBusInternalException(
        "Erro de criptografia com uso de chave pública.", e);
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginByCertificate(String entity, byte[] privateKeyBytes)
    throws InvalidPrivateKey, AlreadyLoggedIn, MissingCertificate,
    AccessDenied, ServiceFailure, BusChanged {
    checkLoggedIn();
    this.manager.ignoreCurrentThread();
    LoginProcess loginProcess = null;
    LoginInfo newLogin;
    try {
      RSAPrivateKey privateKey =
        crypto.createPrivateKeyFromBytes(privateKeyBytes);
      EncryptedBlockHolder challengeHolder = new EncryptedBlockHolder();
      loginProcess =
        getBus().getAccessControl().startLoginByCertificate(entity,
          challengeHolder);
      byte[] decryptedChallenge =
        crypto.decrypt(challengeHolder.value, privateKey);

      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(decryptedChallenge);

      IntHolder validityHolder = new IntHolder();
      newLogin =
        loginProcess.login(this.publicKey.getEncoded(),
          encryptedLoginAuthenticationInfo, validityHolder);
      localLogin(newLogin, validityHolder.value);
    }
    catch (CryptographyException e) {
      loginProcess.cancel();
      throw new AccessDenied("Erro ao descriptografar desafio.");
    }
    catch (WrongEncoding e) {
      throw new OpenBusInternalException(
        "Falhou a codificação com a chave pública do barramento", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new OpenBusInternalException(
        "O Algoritmo de criptografia especificado não existe", e);
    }
    catch (InvalidKeySpecException e) {
      throw new InvalidPrivateKey("Erro ao interpretar bytes da chave privada",
        e);
    }
    catch (InvalidPublicKey e) {
      throw new OpenBusInternalException(
        "Falha no protocolo OpenBus: A chave de acesso gerada não foi aceita. Mensagem="
          + e.message);
    }
    finally {
      this.manager.unignoreCurrentThread();
    }
    logger
      .info(String
        .format(
          "Login por certificado efetuada com sucesso: busid (%s) login (%s) entidade (%s)",
          busid(), newLogin.id, newLogin.entity));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginProcess startSharedAuth(OctetSeqHolder secret)
    throws ServiceFailure {
    EncryptedBlockHolder challenge = new EncryptedBlockHolder();
    LoginProcess process = null;
    Connection previousConnection = manager.getRequester();
    try {
      manager.setRequester(this);
      process = this.access().startLoginBySharedAuth(challenge);
      secret.value = crypto.decrypt(challenge.value, this.privateKey);
    }
    catch (CryptographyException e) {
      process.cancel();
      throw new OpenBusInternalException(
        "Erro ao descriptografar segredo com chave privada.", e);
    }
    finally {
      manager.setRequester(previousConnection);
    }
    return process;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginBySharedAuth(LoginProcess process, byte[] secret)
    throws AlreadyLoggedIn, ServiceFailure, AccessDenied, InvalidLoginProcess,
    BusChanged {
    checkLoggedIn();
    this.manager.ignoreCurrentThread();
    byte[] encryptedLoginAuthenticationInfo =
      this.generateEncryptedLoginAuthenticationInfo(secret);
    IntHolder validity = new IntHolder();
    LoginInfo newLogin;
    try {
      newLogin =
        process.login(this.publicKey.getEncoded(),
          encryptedLoginAuthenticationInfo, validity);
      localLogin(newLogin, validity.value);
    }
    catch (WrongEncoding e) {
      throw new AccessDenied("Erro durante tentativa de login.");
    }
    catch (OBJECT_NOT_EXIST e) {
      throw new InvalidLoginProcess("Objeto de processo de login é inválido");
    }
    catch (InvalidPublicKey e) {
      throw new OpenBusInternalException(
        "Falha no protocolo OpenBus: A chave de acesso gerada não foi aceita. Mensagem="
          + e.message);
    }
    finally {
      this.manager.unignoreCurrentThread();
    }
    logger
      .info(String
        .format(
          "Login por compatilhamento de atutenticação efetuado com sucesso: busid (%s) login (%s) entidade (%s)",
          busid(), newLogin.id, newLogin.entity));
  }

  /**
   * Dispara a thread de renovação de Login
   * 
   * @param defaultLease tempo de lease padrão.
   */
  private void fireRenewerThread(int defaultLease) {
    if (this.renewer != null) {
      this.renewer.stop();
    }
    this.renewer = new LeaseRenewer(this, defaultLease);
    this.renewer.start();
  }

  /**
   * Finaliza a thread de renovação de Login.
   */
  private void stopRenewerThread() {
    if (this.renewer != null) {
      this.renewer.stop();
    }
    this.renewer = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginInfo login() {
    return this.internalLogin.login();
  }

  /**
   * Recupera o login associado a conexão, mas tenta refazer o login via
   * callback, caso a conexão esteja com login inválido.
   * 
   * @return o login.
   */
  LoginInfo getLogin() {
    return this.internalLogin.getLogin();
  }

  /**
   * Realiza o login localmente.
   * 
   * @param newLogin a nova informação de login.
   * @param validity tempo de validade do login.
   * @throws AlreadyLoggedIn se a conexão já estiver logada.
   * @throws BusChanged caso a conexão tente relogar em outro barramento.
   */
  private void localLogin(LoginInfo newLogin, int validity)
    throws AlreadyLoggedIn, BusChanged {
    String old = getBus().getId();
    String busid = getBus().getAccessControl().busid();
    if (!old.equals(busid)) {
      throw new BusChanged(busid);
    }
    writeLock().lock();
    try {
      checkLoggedIn();
      internalLogin.setLoggedIn(newLogin);
    }
    finally {
      writeLock().unlock();
    }
    fireRenewerThread(validity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean logout() throws ServiceFailure {
    LoginInfo login = this.internalLogin.login();
    if (login == null) {
      if (this.internalLogin.invalid() != null) {
        localLogout(false);
      }
      return false;
    }

    Connection previousConnection = manager.getRequester();
    try {
      manager.setRequester(this);
      getBus().getAccessControl().logout();
    }
    catch (NO_PERMISSION e) {
      if (e.minor == InvalidLoginCode.value
        && e.completed.equals(CompletionStatus.COMPLETED_NO)) {
        return false;
      }
      else {
        throw e;
      }
    }
    finally {
      manager.setRequester(previousConnection);
      localLogout(false);
    }
    return true;
  }

  /**
   * Realiza o logout localmente. Se o parâmetro "invalidated" for
   * <code>true</code> seta o estado da conexão para INVÁLIDO, se for
   * <code>false</code> seta o estado para DESLOGADO.
   * 
   * @param invalidated
   */
  void localLogout(boolean invalidated) {
    this.joinedChains.clear();
    stopRenewerThread();
    if (invalidated) {
      this.internalLogin.setInvalid();
    }
    else {
      LoginInfo old = this.internalLogin.setLoggedOut();
      if (old != null) {
        logger.info(String.format("Logout efetuado: id (%s) entidade (%s)",
          old.id, old.entity));
      }
    }
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
    SignedCallChain signedChain;
    try {
      Any any = current.get_slot(mediator.getConnectionSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      String connid = any.extract_string();
      if (!this.connId.equals(connid)) {
        return null;
      }
      any = current.get_slot(mediator.getSignedChainSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      busId = busid();
      signedChain = SignedCallChainHelper.extract(any);
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
    return new CallerChainImpl(busId, callChain.caller, callChain.originators,
      signedChain);
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

    Thread currentThread = Thread.currentThread();
    this.joinedChains.put(currentThread, chain);
    try {
      Current current = ORBUtils.getPICurrent(orb);
      ORBMediator mediator = ORBUtils.getMediator(orb);
      SignedCallChain signedChain = ((CallerChainImpl) chain).signedCallChain();
      Any any = this.orb.create_any();
      SignedCallChainHelper.insert(any, signedChain);
      current.set_slot(mediator.getJoinedChainSlotId(), any);
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
    Thread currentThread = Thread.currentThread();
    this.joinedChains.remove(currentThread);
    try {
      Current current = ORBUtils.getPICurrent(orb);
      ORBMediator mediator = ORBUtils.getMediator(orb);
      Any any = this.orb.create_any();
      current.set_slot(mediator.getJoinedChainSlotId(), any);
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
    Thread currentThread = Thread.currentThread();
    return this.joinedChains.get(currentThread);
  }

  /**
   * Recupera o serviço de controle de acesso.
   * 
   * @return o serviço de controle de acesso.
   */
  AccessControl access() {
    return getBus().getAccessControl();
  }

  /**
   * Recupera o serviço de registro de logins.
   * 
   * @return o serviço de registro de logins.
   */
  LoginRegistry logins() {
    return getBus().getLoginRegistry();
  }

  /**
   * Recupera o serviço de registro de certificados.
   * 
   * @return o serviço de registro de certificados.
   */
  CertificateRegistry certificates() {
    return getBus().getCertificateRegistry();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OfferRegistry offers() {
    return getBus().getOfferRegistry();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidLoginCallback(InvalidLoginCallback callback) {
    this.invalidLoginCallback = callback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InvalidLoginCallback onInvalidLoginCallback() {
    return this.invalidLoginCallback;
  }

  /**
   * Configura as informações de suporte legado do barramento.
   * 
   * @param legacy
   */
  void setLegacyInfo(LegacyInfo legacy) {
    this.legacyBus = legacy;
  }

  /**
   * Verifica se o suporte legado esta ativo.
   * 
   * @return <code>true</code> caso o suporte esteja ativo, e <code>false</code>
   *         caso contrário.
   */
  boolean legacy() {
    if (!this.legacy) {
      return false;
    }
    return this.legacyBus != null;
  }

  /**
   * Recupera a referência para o controle de acesso do suporte legado.
   * 
   * @return o serviço de controle de acesso legado.
   */
  IAccessControlService legacyAccess() {
    return this.legacyBus.getAccessControl();
  }

  /**
   * Retorna
   * 
   * @return bus
   */
  BusInfo getBus() {
    return bus;
  }

  /**
   * Recupera o lock de leitura
   * 
   * @return o lock de leitura.
   */
  ReadLock readLock() {
    return this.readLock;
  }

  /**
   * Recupera o lock de escrita
   * 
   * @return o lock de escrita.
   */
  WriteLock writeLock() {
    return this.writeLock;
  }

  /**
   * Recupera o identificador desta conexão.
   * 
   * @return o identificador da conexão.
   */
  String connId() {
    return this.connId;
  }

  /**
   * Configura o login como inválido.
   */
  void setLoginInvalid() {
    this.internalLogin.setInvalid();
  }

  /**
   * Verifica se a propriedade "legacy.delegate" da conexão está configurada. Os
   * valores possíveis são "originator" e "caller", onde "caller" é o valor
   * default.
   * 
   * @return <code>true</code> se a propriedade esta configurada para
   *         "originator", e <code>false</code> caso contrário.
   */
  boolean isLegacyDelegateSetToOriginator() {
    if (this.delegate.equals("originator")) {
      return true;
    }
    return false;
  }
}
