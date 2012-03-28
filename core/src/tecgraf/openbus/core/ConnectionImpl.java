package tecgraf.openbus.core;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.v2_00.EncryptedBlockHolder;
import tecgraf.openbus.core.v2_00.OctetSeqHolder;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_00.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_00.services.access_control.CertificateRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_00.services.access_control.LoginAuthenticationInfo;
import tecgraf.openbus.core.v2_00.services.access_control.LoginAuthenticationInfoHelper;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_00.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_00.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.CorruptedPrivateKey;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.exception.WrongPrivateKey;
import tecgraf.openbus.exception.WrongSecret;
import tecgraf.openbus.util.Cryptography;

/**
 * Implementação da Interface {@link Connection}
 * 
 * @author Tecgraf
 */
public final class ConnectionImpl implements Connection {
  /** Instância do logger */
  private static final Logger logger = Logger.getLogger(ConnectionImpl.class
    .getName());
  /** Instância auxiliar para tratar de criptografia */
  private Cryptography crypto;

  /** ORB associado a esta conexão */
  private BusORBImpl orb;
  /** Informações sobre o barramento ao qual a conexão pertence */
  private BusInfo bus;
  /** Chave pública do sdk */
  private RSAPublicKey publicKey;
  /** Chave privada do sdk */
  private RSAPrivateKey privateKey;
  /** Indica se a conexão esta fechada (descartada para uso) */
  private volatile boolean closed;
  /** Informação do login associado a esta conexão. */
  private LoginInfo login;
  /** Mapa de thread para cadeia de chamada */
  private Map<Thread, CallerChain> joinedChains;
  /** Thread de renovação de login */
  private LeaseRenewer renewer;
  /** Callback a ser disparada caso o login se encontre inválido */
  private InvalidLoginCallback invalidLoginCallback;

  /**
   * Construtor.
   * 
   * @param bus
   * @param orb
   */
  public ConnectionImpl(BusInfo bus, BusORB orb) {
    this.bus = bus;
    this.orb = (BusORBImpl) orb;
    this.crypto = Cryptography.getInstance();
    KeyPair keyPair;
    try {
      keyPair = crypto.generateRSAKeyPair();
    }
    catch (CryptographyException e) {
      throw new OpenBusInternalException(
        "Erro inexperado na geração do par de chaves.", e);
    }
    this.publicKey = (RSAPublicKey) keyPair.getPublic();
    this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
    this.closed = false;
    this.joinedChains = new HashMap<Thread, CallerChain>();
    ConnectionMultiplexerImpl multiplexer = this.orb.getConnectionMultiplexer();
    multiplexer.addConnection(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ORB orb() {
    return this.orb.getORB();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String busid() {
    return this.bus.getId();
  }

  /**
   * Recupera a chave pública do barramento.
   * 
   * @return a chave pública.
   */
  RSAPublicKey getBusPublicKey() {
    return this.bus.getPublicKey();
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
    if (this.login != null) {
      throw new AlreadyLoggedIn();
    }
  }

  /**
   * Verifica se a conexão esta fechada, e lança uma exceção
   * {@link OpenBusInternalException} caso esteja.
   */
  private void checkClosed() {
    // CHECK confirmar os locais onde devem existir a verificação checkClosed
    if (this.closed) {
      throw new OpenBusInternalException("Conexão fechada!");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws ServiceFailure {
    this.logout();
    this.closed = true;
    this.orb.getConnectionMultiplexer().removeConnection(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginByPassword(String entity, byte[] password)
    throws AccessDenied, AlreadyLoggedIn, ServiceFailure {
    checkClosed();
    checkLoggedIn();
    try {
      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(password);
      IntHolder validityHolder = new IntHolder();
      this.orb.ignoreCurrentThread();
      this.login =
        this.bus.getAccessControl().loginByPassword(entity,
          this.publicKey.getEncoded(), encryptedLoginAuthenticationInfo,
          validityHolder);
    }
    catch (WrongEncoding e) {
      throw new OpenBusInternalException(
        "Falhou a codificação com a chave pública do barramento", e);
    }
    finally {
      this.orb.unignoreCurrentThread();
    }
    logger
      .info(String
        .format(
          "Login por senha efetuado com sucesso: busid (%s) login (%s) entidade (%s)",
          busid(), login.id, login.entity));
    fireRenewerThread();
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
      Any authenticationInfoAny = this.orb.getORB().create_any();
      LoginAuthenticationInfoHelper.insert(authenticationInfoAny,
        authenticationInfo);
      byte[] encodedLoginAuthenticationInfo =
        this.orb.getCodec().encode_value(authenticationInfoAny);
      return crypto.encrypt(encodedLoginAuthenticationInfo, this.bus
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
  public void loginByCertificate(String entity, RSAPrivateKey privateKey)
    throws CorruptedPrivateKey, WrongPrivateKey, AlreadyLoggedIn,
    MissingCertificate, ServiceFailure {
    checkClosed();
    checkLoggedIn();
    this.orb.ignoreCurrentThread();
    try {
      EncryptedBlockHolder challengeHolder = new EncryptedBlockHolder();
      LoginProcess loginProcess =
        this.bus.getAccessControl().startLoginByCertificate(entity,
          challengeHolder);
      byte[] decryptedChallenge =
        crypto.decrypt(challengeHolder.value, privateKey);

      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(decryptedChallenge);

      IntHolder validityHolder = new IntHolder();
      this.login =
        loginProcess.login(this.publicKey.getEncoded(),
          encryptedLoginAuthenticationInfo, validityHolder);
    }
    catch (CryptographyException e) {
      throw new CorruptedPrivateKey("Erro ao descriptografar desafio.", e);
    }
    catch (AccessDenied e) {
      throw new OpenBusInternalException("Desafio enviado difere do esperado.",
        e);
    }
    catch (WrongEncoding e) {
      throw new OpenBusInternalException(
        "Falhou a codificação com a chave pública do barramento", e);
    }
    finally {
      this.orb.unignoreCurrentThread();
    }
    logger
      .info(String
        .format(
          "Login por certificado efetuada com sucesso: busid (%s) login (%s) entidade (%s)",
          busid(), login.id, login.entity));
    fireRenewerThread();
  }

  /**
   * {@inheritDoc}
   * 
   * @throws ServiceFailure
   */
  @Override
  public LoginProcess startSingleSignOn(OctetSeqHolder secret)
    throws ServiceFailure {
    checkClosed();
    EncryptedBlockHolder challenge = new EncryptedBlockHolder();
    LoginProcess process = this.access().startLoginBySingleSignOn(challenge);
    try {
      secret.value = crypto.decrypt(challenge.value, this.privateKey);
    }
    catch (CryptographyException e) {
      process.cancel();
      throw new OpenBusInternalException(
        "Erro ao descriptografar segredo com chave privada.", e);
    }
    return process;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginBySingleSignOn(LoginProcess process, byte[] secret)
    throws WrongSecret, AlreadyLoggedIn, ServiceFailure {
    checkClosed();
    checkLoggedIn();
    byte[] encryptedLoginAuthenticationInfo =
      this.generateEncryptedLoginAuthenticationInfo(secret);
    IntHolder validity = new IntHolder();
    try {
      this.login =
        process.login(this.publicKey.getEncoded(),
          encryptedLoginAuthenticationInfo, validity);
    }
    catch (AccessDenied e) {
      throw new WrongSecret("Erro durante tentativa de login.", e);
    }
    catch (WrongEncoding e) {
      throw new WrongSecret("Erro durante tentativa de login.", e);
    }
    logger
      .info(String
        .format(
          "Login por singleSignOn efetuado com sucesso: busid (%s) login (%s) entidade (%s)",
          busid(), login.id, login.entity));
    fireRenewerThread();
  }

  /**
   * Dispara a thread de renovação de Login
   */
  private void fireRenewerThread() {
    if (this.renewer != null) {
      this.renewer.stop();
    }
    this.renewer = new LeaseRenewer(this);
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
    return this.login;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean logout() throws ServiceFailure {
    checkClosed();
    if (this.login != null) {
      try {
        this.bus.getAccessControl().logout();
        localLogout();
        return true;
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
    }
    return false;
  }

  /**
   * Realiza o logout localmente.
   */
  void localLogout() {
    this.joinedChains.clear();
    stopRenewerThread();
    logger.info(String.format("Logout efetuado: id (%s) entidade (%s)",
      login.id, login.entity));
    this.login = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain getCallerChain() {
    checkClosed();
    return this.orb.getCallerChain();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joinChain() throws OpenBusInternalException {
    checkClosed();
    CallerChain currentChain = this.orb.getCallerChain();
    if (currentChain == null) {
      return;
    }
    joinChain(currentChain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joinChain(CallerChain chain) {
    checkClosed();
    Thread currentThread = Thread.currentThread();
    this.joinedChains.put(currentThread, chain);
    this.orb.joinChain(chain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void exitChain() {
    checkClosed();
    Thread currentThread = Thread.currentThread();
    this.joinedChains.remove(currentThread);
    this.orb.exitChain();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain getJoinedChain() {
    checkClosed();
    Thread currentThread = Thread.currentThread();
    return this.joinedChains.get(currentThread);
  }

  /**
   * Recupera o serviço de controle de acesso.
   * 
   * @return o serviço de controle de acesso.
   */
  AccessControl access() {
    return this.bus.getAccessControl();
  }

  /**
   * Recupera o serviço de registro de logins.
   * 
   * @return o serviço de registro de logins.
   */
  LoginRegistry logins() {
    return this.bus.getLoginRegistry();
  }

  /**
   * Recupera o serviço de registro de certificados.
   * 
   * @return o serviço de registro de certificados.
   */
  CertificateRegistry certificates() {
    return this.bus.getCertificateRegistry();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OfferRegistry offers() {
    checkClosed();
    return this.bus.getOfferRegistry();
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

}
