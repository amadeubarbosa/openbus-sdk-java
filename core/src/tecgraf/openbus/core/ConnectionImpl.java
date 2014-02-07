package tecgraf.openbus.core;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.PrivateKey;
import tecgraf.openbus.core.Session.ClientSideSession;
import tecgraf.openbus.core.Session.ServerSideSession;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.core.v2_0.BusObjectKey;
import tecgraf.openbus.core.v2_0.EncryptedBlockHolder;
import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
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
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.InvalidPropertyValue;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.security.Cryptography;

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
  private OpenBusContextImpl context;
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

  /** Thread de renovação de login */
  private LeaseRenewer renewer;
  /** Callback a ser disparada caso o login se encontre inválido */
  private InvalidLoginCallback invalidLoginCallback;

  /** Caches da conexão */
  Caches cache;

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
   * @throws InvalidPropertyValue Existe uma propriedade com um valor inválido.
   */
  public ConnectionImpl(String host, int port, OpenBusContextImpl manager,
    ORB orb) throws InvalidPropertyValue {
    this(host, port, manager, orb, new Properties());
  }

  /**
   * Construtor.
   * 
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * @param context Implementação do multiplexador de conexão.
   * @param orb ORB que essa conexão ira utilizar;
   * @param props Propriedades da conexão.
   * @throws InvalidPropertyValue Existe uma propriedade com um valor inválido.
   */
  public ConnectionImpl(String host, int port, OpenBusContextImpl context,
    ORB orb, Properties props) throws InvalidPropertyValue {
    if ((host == null) || (host.isEmpty()) || (port < 0)) {
      throw new IllegalArgumentException(
        "Os parametros host e/ou port não são validos");
    }

    this.orb = orb;
    this.context = context;
    this.cache = new Caches(this);
    this.bus = null;
    this.legacyBus = null;
    if (props == null) {
      props = new Properties();
    }
    String prop = OpenBusProperty.LEGACY_DISABLE.getProperty(props);
    Boolean disabled = Boolean.valueOf(prop);
    this.legacy = !disabled;
    this.delegate = OpenBusProperty.LEGACY_DELEGATE.getProperty(props);
    try {
      this.context.ignoreCurrentThread();
      buildCorbaLoc(host, port);
    }
    finally {
      this.context.unignoreCurrentThread();
    }

    try {
      this.crypto = Cryptography.getInstance();
      KeyPair keyPair = crypto.generateRSAKeyPair();
      this.publicKey = (RSAPublicKey) keyPair.getPublic();
      this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
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
   * Constrói o corbaloc para acessar o barramento.
   * 
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   */
  private void buildCorbaLoc(String host, int port) {
    String str =
      String.format("corbaloc::1.0@%s:%d/%s", host, port, BusObjectKey.value);
    org.omg.CORBA.Object obj = orb.string_to_object(str);
    this.bus = new BusInfo(obj);

    if (this.legacy) {
      String legacyStr =
        String.format("corbaloc::1.0@%s:%d/%s", host, port, "openbus_v1_05");
      org.omg.CORBA.Object legacyObj = orb.string_to_object(legacyStr);
      this.legacyBus = new LegacyInfo(legacyObj);
    }
  }

  /**
   * Recupera apenas as referências do barramento necessárias para realizar o
   * login.
   */
  private void initBusReferencesBeforeLogin() {
    this.bus.basicBusInitialization();
    if (this.legacy) {
      this.legacy = this.legacyBus.activateLegacySuport();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginByPassword(String entity, byte[] password)
    throws AccessDenied, AlreadyLoggedIn, ServiceFailure {
    checkLoggedIn();
    LoginInfo newLogin;
    try {
      this.context.ignoreCurrentThread();
      initBusReferencesBeforeLogin();

      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(password);
      IntHolder validityHolder = new IntHolder();
      newLogin =
        this.access().loginByPassword(entity, this.publicKey.getEncoded(),
          encryptedLoginAuthenticationInfo, validityHolder);
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
      this.context.unignoreCurrentThread();
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
  public void loginByCertificate(String entity, PrivateKey privateKey)
    throws AlreadyLoggedIn, MissingCertificate, AccessDenied, ServiceFailure {
    checkLoggedIn();
    this.context.ignoreCurrentThread();
    initBusReferencesBeforeLogin();
    LoginProcess loginProcess = null;
    LoginInfo newLogin;
    OpenBusPrivateKey privKey = (OpenBusPrivateKey) privateKey;
    try {
      EncryptedBlockHolder challengeHolder = new EncryptedBlockHolder();
      loginProcess =
        this.access().startLoginByCertificate(entity, challengeHolder);
      byte[] decryptedChallenge =
        crypto.decrypt(challengeHolder.value, privKey.getRSAPrivateKey());

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
    catch (InvalidPublicKey e) {
      throw new OpenBusInternalException(
        "Falha no protocolo OpenBus: A chave de acesso gerada não foi aceita. Mensagem="
          + e.message);
    }
    finally {
      this.context.unignoreCurrentThread();
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
    Connection previousConnection = context.getCurrentConnection();
    try {
      context.setCurrentConnection(this);
      process = this.access().startLoginBySharedAuth(challenge);
      secret.value = crypto.decrypt(challenge.value, this.privateKey);
    }
    catch (CryptographyException e) {
      process.cancel();
      throw new OpenBusInternalException(
        "Erro ao descriptografar segredo com chave privada.", e);
    }
    finally {
      context.setCurrentConnection(previousConnection);
    }
    return process;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginBySharedAuth(LoginProcess process, byte[] secret)
    throws AlreadyLoggedIn, ServiceFailure, AccessDenied, InvalidLoginProcess {
    checkLoggedIn();
    this.context.ignoreCurrentThread();
    initBusReferencesBeforeLogin();
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
      this.context.unignoreCurrentThread();
    }
    logger
      .info(String
        .format(
          "Login por compatilhamento de autenticação efetuado com sucesso: busid (%s) login (%s) entidade (%s)",
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
   */
  private void localLogin(LoginInfo newLogin, int validity)
    throws AlreadyLoggedIn {
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

    Connection previousConnection = context.getCurrentConnection();
    CallerChain previousChain = context.getJoinedChain();
    try {
      context.exitChain();
      context.setCurrentConnection(this);
      this.access().logout();
    }
    catch (SystemException e) {
      logger.log(Level.WARNING, String.format(
        "Erro durante chamada remota de logout: "
          + "busid (%s) login (%s) entidade (%s)", busid(), login.id,
        login.entity), e);
      return false;
    }
    finally {
      context.setCurrentConnection(previousConnection);
      context.joinChain(previousChain);
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
    this.cache.clear();
    this.bus.clearBusInfos();
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
   * Recupera o serviço de registro.
   * 
   * @return o serviço de registro.
   */
  OfferRegistry offers() {
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

  /**
   * Recupera o próximo indentificador de sessão disponível.
   * 
   * @return o Identificador de sessão.
   */
  int nextAvailableSessionId() {
    synchronized (this.cache.srvSessions) {
      for (int i = 1; i <= this.cache.CACHE_SIZE + 1; i++) {
        if (!this.cache.srvSessions.containsKey(i)) {
          return i;
        }
      }
    }
    // não deveria chegar neste ponto
    return this.cache.CACHE_SIZE + 1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    ConnectionImpl other = null;
    if (obj instanceof ConnectionImpl) {
      other = (ConnectionImpl) obj;
      return this.connId.equals(other.connId);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.connId.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this.connId;
  }

  /**
   * Classe interna para agrupar os caches utilizados pela conexão.
   * 
   * @author Tecgraf
   */
  class Caches {
    /** Tamanho das caches dos interceptadores */
    final int CACHE_SIZE = 30;
    /* Caches Cliente da conexão */
    /** Mapa de profile do interceptador para o cliente alvo da chamanha */
    Map<EffectiveProfile, String> entities;
    /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
    Map<String, ClientSideSession> cltSessions;
    /** Cache de cadeias assinadas */
    Map<ChainCacheKey, SignedCallChain> chains;
    /* Caches servidor da conexão */
    /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
    Map<Integer, ServerSideSession> srvSessions;
    /** Cache de login */
    LoginCache logins;
    /** Cache de validade de credencial 1.5 */
    IsValidCache valids;

    /**
     * Construtor.
     * 
     * @param conn a referência para a conexão ao qual os caches estão
     *        referenciados.
     */
    public Caches(ConnectionImpl conn) {
      this.entities =
        Collections.synchronizedMap(new LRUCache<EffectiveProfile, String>(
          CACHE_SIZE));
      this.cltSessions =
        Collections.synchronizedMap(new LRUCache<String, ClientSideSession>(
          CACHE_SIZE));
      this.chains =
        Collections
          .synchronizedMap(new LRUCache<ChainCacheKey, SignedCallChain>(
            CACHE_SIZE));
      this.srvSessions =
        Collections.synchronizedMap(new LRUCache<Integer, ServerSideSession>(
          CACHE_SIZE));
      this.logins = new LoginCache(conn, CACHE_SIZE);
      this.valids = new IsValidCache(conn, CACHE_SIZE);
    }

    /**
     * Limpa as caches.
     */
    protected void clear() {
      this.entities.clear();
      this.cltSessions.clear();
      this.chains.clear();
      this.srvSessions.clear();
      this.logins.clear();
      this.valids.clear();
    }
  }
}
