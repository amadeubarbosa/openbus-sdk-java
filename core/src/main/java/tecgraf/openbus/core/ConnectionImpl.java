package tecgraf.openbus.core;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
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
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.Credential.Chain;
import tecgraf.openbus.core.Session.ClientSideSession;
import tecgraf.openbus.core.Session.ServerSideSession;
import tecgraf.openbus.core.v2_0.services.access_control.AccessControlHelper;
import tecgraf.openbus.core.v2_1.EncryptedBlockHolder;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidPublicKey;
import tecgraf.openbus.core.v2_1.services.access_control.LoginAuthenticationInfo;
import tecgraf.openbus.core.v2_1.services.access_control.LoginAuthenticationInfoHelper;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_1.services.legacy_support.LegacyConverter;
import tecgraf.openbus.core.v2_1.services.legacy_support.LegacyConverterHelper;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.InvalidPropertyValue;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.exception.WrongBus;
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

  /* Suporte Legado. */
  /** Informa se o suporte legado esta ativo */
  private boolean legacy;
  /** Suporte legado */
  private LegacySupport legacySupport;

  /**
   * Construtor.
   * 
   * @param reference referêcia para o barramento
   * @param manager Implementação do multiplexador de conexão.
   * @param orb ORB que essa conexão ira utilizar;
   * @throws InvalidPropertyValue Existe uma propriedade com um valor inválido.
   */
  public ConnectionImpl(org.omg.CORBA.Object reference,
    OpenBusContextImpl manager, ORB orb) throws InvalidPropertyValue {
    this(reference, manager, orb, new Properties());
  }

  /**
   * Construtor.
   * 
   * @param reference referêcia para o barramento
   * @param context Implementação do multiplexador de conexão.
   * @param orb ORB que essa conexão ira utilizar;
   * @param props Propriedades da conexão.
   * @throws InvalidPropertyValue Existe uma propriedade com um valor inválido.
   */
  public ConnectionImpl(org.omg.CORBA.Object reference,
    OpenBusContextImpl context, ORB orb, Properties props)
    throws InvalidPropertyValue {
    this.orb = orb;
    this.context = context;
    this.bus = new BusInfo(reference);
    if (props == null) {
      props = new Properties();
    }
    String prop = OpenBusProperty.LEGACY_DISABLE.getProperty(props);
    Boolean disabled = Boolean.valueOf(prop);
    this.legacy = !disabled;

    // verificando por valor de tamanho de cache
    String ssize = OpenBusProperty.CACHE_SIZE.getProperty(props);
    try {
      int size = Integer.parseInt(ssize);
      if (size <= 0) {
        throw new InvalidPropertyValue(OpenBusProperty.CACHE_SIZE.getKey(),
          ssize);
      }
      this.cache = new Caches(this, size);
    }
    catch (NumberFormatException e) {
      throw new InvalidPropertyValue(OpenBusProperty.CACHE_SIZE.getKey(),
        ssize, e);
    }

    // verificando por definicao de chaves em propriedades
    String path = OpenBusProperty.ACCESS_KEY.getProperty(props);
    KeyPair keyPair;
    if (path != null) {
      try {
        this.crypto = Cryptography.getInstance();
        keyPair = crypto.readKeyPairFromFile(path);
      }
      catch (InvalidKeySpecException e) {
        throw new InvalidPropertyValue(OpenBusProperty.ACCESS_KEY.getKey(),
          path, e);
      }
      catch (IOException e) {
        throw new InvalidPropertyValue(OpenBusProperty.ACCESS_KEY.getKey(),
          path, e);
      }
      catch (CryptographyException e) {
        throw new OpenBusInternalException(
          "Erro inexperado ao carregar chave privada.", e);
      }
    }
    else {
      // cria par de chaves
      try {
        this.crypto = Cryptography.getInstance();
        keyPair = crypto.generateRSAKeyPair();
      }
      catch (CryptographyException e) {
        throw new OpenBusInternalException(
          "Erro inexperado na geração do par de chaves.", e);
      }
    }
    this.publicKey = (RSAPublicKey) keyPair.getPublic();
    this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
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
   * Recupera apenas as referências do barramento necessárias para realizar o
   * login.
   */
  private void initBusReferencesBeforeLogin() {
    this.bus.basicBusInitialization();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginByPassword(String entity, byte[] password, String domain)
    throws AccessDenied, AlreadyLoggedIn, TooManyAttempts, UnknownDomain,
    WrongEncoding, ServiceFailure {
    checkLoggedIn();
    LoginInfo newLogin;
    try {
      this.context.ignoreThread();
      initBusReferencesBeforeLogin();

      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(password);
      IntHolder validityHolder = new IntHolder();
      newLogin =
        this.access().loginByPassword(entity, domain,
          this.publicKey.getEncoded(), encryptedLoginAuthenticationInfo,
          validityHolder);
      localLogin(newLogin, validityHolder.value);
    }
    catch (InvalidPublicKey e) {
      throw new OpenBusInternalException(
        "Falha no protocolo OpenBus: A chave de acesso gerada não foi aceita. Mensagem="
          + e.message);
    }
    finally {
      this.context.unignoreThread();
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
   * 
   */
  @Override
  public void loginByCertificate(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedIn, MissingCertificate, AccessDenied, WrongEncoding,
    ServiceFailure {
    checkLoggedIn();
    this.context.ignoreThread();
    initBusReferencesBeforeLogin();
    LoginProcess loginProcess = null;
    LoginInfo newLogin;
    try {
      EncryptedBlockHolder challengeHolder = new EncryptedBlockHolder();
      loginProcess =
        this.access().startLoginByCertificate(entity, challengeHolder);
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
    catch (InvalidPublicKey e) {
      throw new OpenBusInternalException(
        "Falha no protocolo OpenBus: A chave de acesso gerada não foi aceita. Mensagem="
          + e.message);
    }
    finally {
      this.context.unignoreThread();
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
  public SharedAuthSecret startSharedAuth() throws ServiceFailure {
    EncryptedBlockHolder challenge = new EncryptedBlockHolder();
    LoginProcess process = null;
    byte[] secret = null;
    tecgraf.openbus.core.v2_0.services.access_control.LoginProcess legacyProcess =
      null;
    Connection previousConnection = context.getCurrentConnection();
    try {
      context.setCurrentConnection(this);
      process = this.access().startLoginBySharedAuth(challenge);
      secret = crypto.decrypt(challenge.value, this.privateKey);
      if (legacy() && legacySupport().converter() != null) {
        try {
          legacyProcess =
            legacySupport().converter().convertSharedAuth(process);
        }
        catch (Exception e) {
          String msg =
            "Não foi possível converter o compartilhamento de autenticação";
          logger.log(Level.SEVERE, msg, e);
        }
      }
    }
    catch (CryptographyException e) {
      process.cancel();
      throw new OpenBusInternalException(
        "Erro ao descriptografar segredo com chave privada.", e);
    }
    finally {
      context.setCurrentConnection(previousConnection);
    }
    return new SharedAuthSecretImpl(busid(), process, legacyProcess, secret,
      context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginBySharedAuth(SharedAuthSecret secret)
    throws AlreadyLoggedIn, WrongBus, ServiceFailure, AccessDenied,
    InvalidLoginProcess {
    checkLoggedIn();
    LoginInfo newLogin;
    try {
      this.context.ignoreThread();
      initBusReferencesBeforeLogin();
      if (this.busid().equals(secret.busid())) {
        SharedAuthSecretImpl sharedAuth = (SharedAuthSecretImpl) secret;
        byte[] encryptedLoginAuthenticationInfo =
          this.generateEncryptedLoginAuthenticationInfo(sharedAuth.secret());
        IntHolder validity = new IntHolder();
        newLogin =
          sharedAuth.attempt().login(this.publicKey.getEncoded(),
            encryptedLoginAuthenticationInfo, validity);
        localLogin(newLogin, validity.value);
      }
      else {
        throw new WrongBus();
      }
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
      this.context.unignoreThread();
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
    if (legacy) {
      activateLegacySupport();
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
      return true;
    }

    Connection previousConnection = context.getCurrentConnection();
    CallerChain previousChain = context.getJoinedChain();
    try {
      context.exitChain();
      context.setCurrentConnection(this);
      context.ignoreInvLogin();
      this.access().logout();
    }
    catch (NO_PERMISSION e) {
      if (e.minor == InvalidLoginCode.value) {
        // ignore error. Result is the same of a successful call to logout
      }
      else {
        logger.log(Level.WARNING, String.format(
          "Erro durante chamada remota de logout: "
            + "busid (%s) login (%s) entidade (%s)", busid(), login.id,
          login.entity), e);
        return false;
      }
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
      context.unignoreInvLogin();
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
   * Verifica se o suporte legado está ativo.
   * <p>
   * Mesmo que a conexão tenha sido configurada para permitir o suporte legado
   * 
   * @return <code>true</code> caso o suporte esteja ativo, e <code>false</code>
   *         caso contrário.
   */
  public boolean legacy() {
    if (legacy) {
      return this.legacySupport != null;
    }
    return false;
  }

  /**
   * Recupera os serviços de apoio ao suporte legado.
   * 
   * @return infra de suporte legado.
   */
  LegacySupport legacySupport() {
    return legacySupport;
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
   * Ativa o suporte legado recuperando referência para os serviços necessários.
   */
  private void activateLegacySupport() {
    org.omg.CORBA.Object object =
      bus.getComponent().getFacetByName("LegacySupport");
    if (object != null) {
      IComponent comp = IComponentHelper.narrow(object);
      org.omg.CORBA.Object faccess = comp.getFacet(AccessControlHelper.id());
      if (faccess != null) {
        tecgraf.openbus.core.v2_0.services.access_control.AccessControl access =
          AccessControlHelper.narrow(faccess);
        org.omg.CORBA.Object fconv = comp.getFacet(LegacyConverterHelper.id());
        LegacyConverter converter = null;
        if (fconv != null) {
          converter = LegacyConverterHelper.narrow(fconv);
        }
        else {
          logger.warning("Exportação de dado legado desativado.");
        }
        legacySupport = new LegacySupport(access, converter);
      }
      else {
        legacy = false;
        logger
          .severe("Suporte legado desativado dado ausência de controle de acesso");
      }
    }
    else {
      legacy = false;
      logger.warning("Suporte legado não disponível");
    }
  }

  /**
   * Classe interna para agrupar os caches utilizados pela conexão.
   * 
   * @author Tecgraf
   */
  class Caches {
    /** Tamanho das caches dos interceptadores */
    final int CACHE_SIZE;
    /* Caches Cliente da conexão */
    /** Mapa de profile do interceptador para o cliente alvo da chamanha */
    Map<EffectiveProfile, String> entities;
    /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
    Map<String, ClientSideSession> cltSessions;
    /** Cache de cadeias assinadas */
    Map<ChainCacheKey, Chain> chains;
    /* Caches servidor da conexão */
    /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
    Map<Integer, ServerSideSession> srvSessions;
    /** Cache de login */
    LoginCache logins;

    /**
     * Construtor.
     * 
     * @param conn a referência para a conexão ao qual os caches estão
     *        referenciados.
     * @param size tamanho de cada cache
     */
    public Caches(ConnectionImpl conn, int size) {
      this.CACHE_SIZE = size;
      this.entities =
        Collections.synchronizedMap(new LRUCache<EffectiveProfile, String>(
          CACHE_SIZE));
      this.cltSessions =
        Collections.synchronizedMap(new LRUCache<String, ClientSideSession>(
          CACHE_SIZE));
      this.chains =
        Collections.synchronizedMap(new LRUCache<ChainCacheKey, Chain>(
          CACHE_SIZE));
      this.srvSessions =
        Collections.synchronizedMap(new LRUCache<Integer, ServerSideSession>(
          CACHE_SIZE));
      this.logins = new LoginCache(conn, CACHE_SIZE);
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
    }
  }
}
