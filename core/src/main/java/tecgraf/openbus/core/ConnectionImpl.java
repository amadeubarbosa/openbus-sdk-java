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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.util.concurrent.Uninterruptibles;
import org.omg.CORBA.Any;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import org.omg.PortableServer.POA;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.*;
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
import tecgraf.openbus.retry.RetryTaskPool;
import tecgraf.openbus.security.Cryptography;

/**
 * Implementação da Interface {@link Connection}
 * 
 * @author Tecgraf
 */
final class ConnectionImpl implements Connection, InvalidLoginCallback {

  /** Identificador da conexão */
  private final String connId = UUID.randomUUID().toString();
  /** Instância do logger */
  private static final Logger logger = Logger.getLogger(ConnectionImpl.class
    .getName());
  /** Instância auxiliar para tratar de criptografia */
  private Cryptography crypto;

  /** ORB associado a esta conexão */
  private final ORB orb;
  /** POA que essa conexão deve utilizar. Aqui na conexão esse objeto é final
   para evitar contenções. */
  private final POA poa;
  /** Gerente da conexão. */
  private OpenBusContextImpl context;
  /** Informações sobre o barramento ao qual a conexão pertence */
  private BusInfo bus;
  /** Registro de logins local */
  private final LoginRegistryImpl loginRegistry;
  /** Registro de ofertas local */
  private final OfferRegistryImpl offerRegistry;
  /** Intervalo de tempo entre tentativas */
  private final long interval;
  /** Unidade de tempo do intervalo */
  private final TimeUnit unit;
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
  /** Callback com dados de autenticação para login */
  private LoginCallback cb;

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
   * @param context Implementação do multiplexador de conexão.
   * @param orb ORB que essa conexão ira utilizar;
   * @param poa POA que essa conexão ira utilizar;
   * @param props Propriedades da conexão.
   * @throws InvalidPropertyValue Existe uma propriedade com um valor inválido.
   */
  ConnectionImpl(org.omg.CORBA.Object reference,
    OpenBusContextImpl context, ORB orb, POA poa, Properties props)
    throws InvalidPropertyValue {
    this.orb = orb;
    this.poa = poa;
    this.context = context;
    this.bus = new BusInfo(reference);
    if (props == null) {
      props = new Properties();
    }
    String threads = OpenBusProperty.THREAD_NUM.getProperty(props);
    int threadNum;
    if (threads == null) {
      threadNum = 10;
    } else {
      threadNum = Integer.parseInt(threads);
    }
    if (threadNum <= 0) {
      throw new InvalidPropertyValue(OpenBusProperty.THREAD_NUM.getKey(),
        threads);
    }
    RetryTaskPool pool = new RetryTaskPool(threadNum);
    String interval = OpenBusProperty.TIME_INTERVAL.getProperty(props);
    if (interval == null) {
      this.interval = 1000;
    } else {
      this.interval = Long.parseLong(interval);
    }
    if (this.interval < 0) {
      throw new InvalidPropertyValue(OpenBusProperty.TIME_INTERVAL.getKey(),
        interval);
    }
    String unit = OpenBusProperty.TIME_UNIT.getProperty(props);
    if (unit == null || unit.equals("ms") || unit.equals("millis") || unit
      .equals("milliseconds")) {
      this.unit = TimeUnit.MILLISECONDS;
    } else if (unit.equals("s") || unit.equals("secs") || unit
        .equals("seconds")) {
        this.unit = TimeUnit.SECONDS;
    } else if (unit.equals("ns") || unit.equals("nanos") || unit
      .equals("nanoseconds")) {
        this.unit = TimeUnit.NANOSECONDS;
    } else if (unit.equals("m") || unit.equals("mins") || unit
    .equals("minutes")) {
      this.unit = TimeUnit.MINUTES;
    } else {
      throw new InvalidPropertyValue(OpenBusProperty.TIME_UNIT.getKey(),
        unit);
    }

    this.loginRegistry = new LoginRegistryImpl(context, this, poa, pool,
      this.interval, this.unit);
    this.offerRegistry = new OfferRegistryImpl(context, this, poa, pool,
      this.interval, this.unit);

    String prop = OpenBusProperty.LEGACY_DISABLE.getProperty(props);
    Boolean disabled = Boolean.valueOf(prop);
    this.legacy = !disabled;

    // verificando por valor de tamanho de cache
    String ssize = OpenBusProperty.CACHE_SIZE.getProperty(props);
    try {
      int size;
      if (ssize == null) {
        size = 30;
      } else {
        size = Integer.parseInt(ssize);
      }
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
      catch (InvalidKeySpecException | IOException e) {
        throw new InvalidPropertyValue(OpenBusProperty.ACCESS_KEY.getKey(),
          path, e);
      } catch (CryptographyException e) {
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
  public POA poa() {
    return this.poa;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OpenBusContext context() {
    return this.context;
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
  public void loginByPassword(final String login, final byte[] password, final String
    domain) throws AccessDenied, AlreadyLoggedIn, TooManyAttempts,
    UnknownDomain, WrongEncoding, ServiceFailure {
    try {
      loginByCallback(() -> new AuthArgs(login, password, domain));
    } catch (WrongBus | InvalidLoginProcess e) {
      throw new OpenBusInternalException("Exceção de login por autenticação " +
        "compartilhada recebida ao realizar login por senha.", e);
    } catch (MissingCertificate e) {
      throw new OpenBusInternalException("Exceção de login por chave privada " +
        "recebida ao realizar login por senha.", e);
    }
  }

  private void loginByPasswordPvt(String entity, byte[] password, String
    domain, LoginCallback cb, boolean relogin)
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
      localLogin(newLogin, validityHolder.value, cb, relogin);
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
  public void loginByPrivateKey(final String entity, final RSAPrivateKey key) throws
    AlreadyLoggedIn, AccessDenied, MissingCertificate, WrongEncoding,
    ServiceFailure {
    try {
      loginByCallback(() -> new AuthArgs(entity, key));
    } catch (WrongBus | InvalidLoginProcess e) {
      throw new OpenBusInternalException("Exceção de login por autenticação " +
        "compartilhada recebida ao realizar login por chave privada.", e);
    } catch (TooManyAttempts | UnknownDomain e) {
      throw new OpenBusInternalException("Exceção de login por senha " +
        "recebida ao realizar login por chave privada.", e);
    }
  }

  private void loginByPrivateKeyPvt(String entity, RSAPrivateKey privateKey,
                                    LoginCallback cb, boolean relogin)
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
      localLogin(newLogin, validityHolder.value, cb, relogin);
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
  public void loginByCallback(LoginCallback cb) throws AlreadyLoggedIn,
    WrongBus, InvalidLoginProcess, AccessDenied, TooManyAttempts,
    UnknownDomain, WrongEncoding, MissingCertificate, ServiceFailure {
    loginByCallback(cb, false);
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
   * Efetua login de uma entidade usando autenticação compartilhada.
   * <p>
   * A autenticação compartilhada é feita a partir de um segredo obtido através
   * da operação {@link #startSharedAuth() startSharedAuth} de uma conexão
   * autenticada.
   *
   * @param secret Segredo a ser fornecido na conclusão do processo de login.
   *
   * @exception InvalidLoginProcess A tentativa de login associada ao segredo
   *            informado é inválido, por exemplo depois do segredo ser
   *            cancelado, ter expirado, ou já ter sido utilizado.
   * @exception AlreadyLoggedIn A conexão já está autenticada.
   * @exception WrongBus O segredo não pertence ao barramento contactado.
   * @exception AccessDenied O segredo fornecido não corresponde ao esperado
   *            pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   */
  private void loginBySharedAuth(SharedAuthSecret secret, LoginCallback cb,
                                 boolean relogin)
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
        if (sharedAuth.attempt() != null) {
          newLogin =
            sharedAuth.attempt().login(this.publicKey.getEncoded(),
              encryptedLoginAuthenticationInfo, validity);
        }
        else {
          tecgraf.openbus.core.v2_0.services.access_control.LoginInfo legacy =
            sharedAuth.legacy().login(this.publicKey.getEncoded(),
              encryptedLoginAuthenticationInfo, validity);
          newLogin = new LoginInfo(legacy.id, legacy.entity);
        }
        localLogin(newLogin, validity.value, cb, relogin);
      }
      else {
        throw new WrongBus();
      }
    }
    catch (WrongEncoding | tecgraf.openbus.core.v2_0.services.access_control.WrongEncoding e) {
      throw new AccessDenied("Erro durante tentativa de login.");
    } catch (OBJECT_NOT_EXIST e) {
      throw new InvalidLoginProcess("Objeto de processo de login é inválido");
    }
    catch (InvalidPublicKey e) {
      throw new OpenBusInternalException(
        "Falha no protocolo OpenBus: A chave de acesso gerada não foi aceita. Mensagem="
          + e.message);
    }
    catch (tecgraf.openbus.core.v2_0.services.access_control.InvalidPublicKey e) {
      throw new OpenBusInternalException(
        "Falha no protocolo OpenBus: A chave de acesso gerada não foi aceita. Mensagem="
          + e.message);
    }
    catch (tecgraf.openbus.core.v2_0.services.access_control.AccessDenied e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      throw new AccessDenied(e.getMessage());
    }
    catch (tecgraf.openbus.core.v2_0.services.ServiceFailure e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      throw new ServiceFailure(e.getMessage(), e.message);
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

  private void loginByCallback(LoginCallback cb, boolean relogin) throws
    AlreadyLoggedIn, WrongBus, InvalidLoginProcess, AccessDenied,
    TooManyAttempts, UnknownDomain, WrongEncoding, MissingCertificate,
    ServiceFailure {
    AuthArgs args = cb.login();
    switch (args.mode) {
      case AuthByPassword:
        loginByPasswordPvt(args.entity, args.password, args.domain, cb,
          relogin);
        break;
      case AuthByPrivateKey:
        loginByPrivateKeyPvt(args.entity, args.privkey, cb, relogin);
        break;
      case AuthBySharedSecret:
        loginBySharedAuth(args.secret, cb, relogin);
        break;
    }
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public void invalidLogin(LoginInfo loginInfo) {
    logger.info("Refazendo login.");
    boolean retry = true;
    boolean loggedOut = true;
    if (login() != null) {
      // já possui um login válido
      retry = false;
      loggedOut = false;
    }
    while (retry && loggedOut) {
      try {
        LoginCallback cb;
        this.readLock.lock();
        try {
          cb = this.cb;
        }
        finally {
          this.readLock.unlock();
        }
        loginByCallback(cb, true);
        retry = false;
      } catch (AlreadyLoggedIn e) {
        retry = false;
      } catch (Exception e) {
        logger.warning("Erro ao tentar refazer o login. " + e);
        retry = true;
      }
      if (retry) {
        Uninterruptibles.sleepUninterruptibly(interval, unit);
      }
      loggedOut = login() == null;
    }
    logger.info("Login refeito.");
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public tecgraf.openbus.LoginRegistry loginRegistry() {
    return loginRegistry;
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public tecgraf.openbus.OfferRegistry offerRegistry() {
    return offerRegistry;
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
  private void localLogin(LoginInfo newLogin, int validity, LoginCallback cb,
                          boolean relogin)
    throws AlreadyLoggedIn {
    if (legacy) {
      activateLegacySupport();
    }
    writeLock().lock();
    try {
      checkLoggedIn();
      internalLogin.setLoggedIn(newLogin);
      this.cb = cb;
      if (relogin) {
        loginRegistry.fireEvent(LoginEvent.RELOGIN, newLogin);
        offerRegistry.fireEvent(LoginEvent.RELOGIN, newLogin);
      } else {
        loginRegistry.fireEvent(LoginEvent.LOGGED_IN, newLogin);
        offerRegistry.fireEvent(LoginEvent.LOGGED_IN, newLogin);
      }
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
    LoginInfo login;
    // adianta a finalização da thread de renovação de login
    this.writeLock().lock();
    try {
      stopRenewerThread();
      login = this.internalLogin.login();
      if (login == null) {
        if (this.internalLogin.invalid() != null) {
          localLogout(false);
        }
        return true;
      }
    }
    finally {
      this.writeLock().unlock();
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
      // ignora erro se for invalidlogin. O resultado é o mesmo de uma
      // chamada bem-sucedida a logout
      if (e.minor != InvalidLoginCode.value) {
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
   * @param invalidated indica se o login está inválido
   */
  void localLogout(boolean invalidated) {
    this.writeLock.lock();
    try {
      this.cache.clear();
      this.bus.clearBusInfos();
      stopRenewerThread();
      if (invalidated) {
        this.internalLogin.setInvalid();
      }
      else {
        LoginInfo old = this.internalLogin.setLoggedOut();
        loginRegistry.fireEvent(LoginEvent.LOGGED_OUT, null);
        offerRegistry.fireEvent(LoginEvent.LOGGED_OUT, null);
        if (old != null) {
          logger.info(String.format("Logout efetuado: id (%s) entidade (%s)",
            old.id, old.entity));
        }
      }
    }
    finally {
      this.writeLock.unlock();
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
    return legacy && this.legacySupport != null;
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
    ConnectionImpl other;
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
    final Map<EffectiveProfile, String> entities;
    /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
    final Map<String, ClientSideSession> cltSessions;
    /** Cache de cadeias assinadas */
    final Map<ChainCacheKey, Chain> chains;
    /* Caches servidor da conexão */
    /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
    final Map<Integer, ServerSideSession> srvSessions;
    /** Cache de login */
    final LoginCache logins;

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
        Collections.synchronizedMap(new LRUCache<>(
          CACHE_SIZE));
      this.cltSessions =
        Collections.synchronizedMap(new LRUCache<>(
          CACHE_SIZE));
      this.chains =
        Collections.synchronizedMap(new LRUCache<>(
          CACHE_SIZE));
      this.srvSessions =
        Collections.synchronizedMap(new LRUCache<>(
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
