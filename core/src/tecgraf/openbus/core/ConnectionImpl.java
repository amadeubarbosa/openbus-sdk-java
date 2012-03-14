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

import tecgraf.openbus.Bus;
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
  private static final Logger logger = Logger.getLogger(ConnectionImpl.class
    .getName());

  /** Informações sobre o barramento ao qual a conexão pertence */
  private BusImpl bus;
  /** Chave pública do sdk */
  private RSAPublicKey publicKey;
  /** Chave privada do sdk */
  private RSAPrivateKey privateKey;
  /** Indica se a conexão esta fechada (descartada para uso) */
  private boolean closed;
  /** Informação do login associado a esta conexão. */
  private LoginInfo login;
  /** Mapa de thread para cadeia de chamada */
  private Map<Thread, CallerChain> joinedChains;
  /** Thread de renovação de login */
  private LeaseRenewer renewer;

  /**
   * Construtor.
   * 
   * @param bus
   * @throws CryptographyException
   */
  ConnectionImpl(Bus bus) throws CryptographyException {
    this.bus = (BusImpl) bus;
    KeyPair keyPair = Cryptography.getInstance().generateRSAKeyPair();
    this.publicKey = (RSAPublicKey) keyPair.getPublic();
    this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
    this.closed = false;
    this.joinedChains = new HashMap<Thread, CallerChain>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ORB orb() {
    return this.bus.getORB().getORB();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String busid() {
    return this.bus.getId();
  }

  RSAPublicKey getBusPublicKey() {
    return this.bus.getPublicKey();
  }

  RSAPrivateKey getPrivateKey() {
    return this.privateKey;
  }

  /**
   * {@inheritDoc}
   */
  // TODO inserir na interface
  public boolean isClosed() {
    return this.closed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws ServiceFailure {
    /*
     * TODO Quando a conexão fecha deve-se remover as threads que utilizam essa
     * conexão do mapa threadedConnectios
     */
    this.logout();
    this.closed = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginByPassword(String entity, byte[] password)
    throws AccessDenied, AlreadyLoggedIn, ServiceFailure {
    logger.fine(String.format(
      "Iniciando a autenticação através de senha para a entidade %s", entity));
    try {
      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(password);
      IntHolder validityHolder = new IntHolder();
      this.bus.getORB().ignoreCurrentThread();
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
      this.bus.getORB().unignoreCurrentThread();
    }
    logger.info(String.format(
      "Autenticação através de senha efetuada com sucesso para a entidade %s",
      entity));
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
      Cryptography crypto = Cryptography.getInstance();
      byte[] publicKeyHash = crypto.generateHash(this.publicKey.getEncoded());

      LoginAuthenticationInfo authenticationInfo =
        new LoginAuthenticationInfo(publicKeyHash, data);
      Any authenticationInfoAny = this.bus.getORB().getORB().create_any();
      LoginAuthenticationInfoHelper.insert(authenticationInfoAny,
        authenticationInfo);
      byte[] encodedLoginAuthenticationInfo =
        this.bus.getORB().getCodec().encode_value(authenticationInfoAny);
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
    this.bus.getORB().ignoreCurrentThread();
    try {
      EncryptedBlockHolder challengeHolder = new EncryptedBlockHolder();
      LoginProcess loginProcess =
        this.bus.getAccessControl().startLoginByCertificate(entity,
          challengeHolder);
      byte[] decryptedChallenge =
        Cryptography.getInstance().decrypt(challengeHolder.value, privateKey);

      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(decryptedChallenge);

      IntHolder validityHolder = new IntHolder();
      this.login =
        loginProcess.login(this.publicKey.getEncoded(),
          encryptedLoginAuthenticationInfo, validityHolder);
    }
    catch (CryptographyException e) {
      throw new OpenBusInternalException("Erro ao descriptografar desafio.", e);
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
      this.bus.getORB().unignoreCurrentThread();
    }
    logger.info(String.format(
      "Autenticação por certificado a efetuada com sucesso para a entidade %s",
      entity));
    fireRenewerThread();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginProcess startSingleSignOn(OctetSeqHolder secret) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginBySingleSignOn(LoginProcess process, byte[] secret)
    throws WrongSecret, AlreadyLoggedIn, ServiceFailure {
    return;
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
    if (this.login != null) {
      try {
        this.bus.getAccessControl().logout();
        localLogout();
        return true;
      }
      catch (NO_PERMISSION e) {
        if (e.minor == InvalidLoginCode.value
          && e.completed.equals(CompletionStatus.COMPLETED_NO)) {
          // chamada de logout não foi realizada. Retorna false.
        }
      }
    }
    return false;
  }

  /**
   * Realiza o logout localmente.
   */
  void localLogout() {
    // TODO: reset caches
    stopRenewerThread();
    this.login = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain getCallerChain() {
    return this.bus.getORB().getCallerChain();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joinChain() throws OpenBusInternalException {
    CallerChain currentChain = this.bus.getORB().getCallerChain();
    if (currentChain == null) {
      return;
    }
    Thread currentThread = Thread.currentThread();
    this.joinedChains.put(currentThread, currentChain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joinChain(CallerChain chain) {
    Thread currentThread = Thread.currentThread();
    this.joinedChains.put(currentThread, chain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void exitChain() {
    Thread currentThread = Thread.currentThread();
    this.joinedChains.remove(currentThread);
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
    return this.bus.getOfferRegistry();
  }

  @Override
  public void onInvalidLoginCallback(InvalidLoginCallback callback) {
    // TODO Auto-generated method stub

  }

  @Override
  public InvalidLoginCallback onInvalidLoginCallback() {
    // TODO Auto-generated method stub
    return null;
  }

}
