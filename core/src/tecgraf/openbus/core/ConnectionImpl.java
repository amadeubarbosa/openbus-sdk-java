package tecgraf.openbus.core;

import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import tecgraf.openbus.exception.CorruptedPrivateKey;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.exception.WrongPrivateKey;
import tecgraf.openbus.exception.WrongSecret;
import tecgraf.openbus.util.Cryptography;

/**
 * Implementação da Interface {@link Connection}
 * 
 * @author Tecgraf
 */
final class ConnectionImpl implements Connection {
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
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * @param manager Implementação do multiplexador de conexão.
   * @param orb ORB que essa conexão ira utilizar;
   */
  public ConnectionImpl(String host, int port, ConnectionManagerImpl manager,
    ORB orb) {

    if ((host == null) || (host.isEmpty()) || (port < 0)) {
      throw new InvalidParameterException(
        "Os parametros host e/ou port não são validos");
    }

    this.orb = orb;
    this.manager = manager;
    this.bus = null;
    this.legacyBus = null;

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
    if (this.login != null) {
      throw new AlreadyLoggedIn();
    }
  }

  /**
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   */
  private void retrieveBusReferences(String host, int port) {
    String str =
      String.format("corbaloc::1.0@%s:%d/%s", host, port, BusObjectKey.value);
    org.omg.CORBA.Object obj = orb.string_to_object(str);
    assert (obj != null);

    IComponent acsComponent = IComponentHelper.narrow(obj);
    assert (acsComponent != null);
    this.bus = new BusInfo(acsComponent);

    /* *
     * enquanto não definimos uma API o legacy esta guardado no ORBMediator
     * porém, esta sempre true. Este é o ponto de entrada para configurar o
     * legacy se for por conexão. Caso seja por ORB, colocar no ORBInit?
     * 
     * TODO: Tirar duvida desse comentario com hroenick.
     */
    String legacyStr =
      String.format("corbaloc::1.0@%s:%d/%s", host, port, "openbus_v1_05");
    org.omg.CORBA.Object legacyObj = orb.string_to_object(legacyStr);
    IComponent acsLegacyComponent = IComponentHelper.narrow(legacyObj);
    this.legacyBus = new LegacyInfo(acsLegacyComponent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loginByPassword(String entity, byte[] password)
    throws AccessDenied, AlreadyLoggedIn, ServiceFailure {
    checkLoggedIn();
    try {
      this.manager.ignoreCurrentThread();

      this.bus.retrieveBusIdAndKey();

      byte[] encryptedLoginAuthenticationInfo =
        this.generateEncryptedLoginAuthenticationInfo(password);
      IntHolder validityHolder = new IntHolder();

      this.login =
        getBus().getAccessControl().loginByPassword(entity,
          this.publicKey.getEncoded(), encryptedLoginAuthenticationInfo,
          validityHolder);
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
   * @throws InvalidPublicKey
   */
  @Override
  public void loginByCertificate(String entity, byte[] privateKeyBytes)
    throws CorruptedPrivateKey, WrongPrivateKey, AlreadyLoggedIn,
    MissingCertificate, ServiceFailure {
    checkLoggedIn();
    this.manager.ignoreCurrentThread();
    try {
      this.bus.retrieveBusIdAndKey();

      RSAPrivateKey privateKey =
        crypto.createPrivateKeyFromBytes(privateKeyBytes);
      EncryptedBlockHolder challengeHolder = new EncryptedBlockHolder();
      LoginProcess loginProcess =
        getBus().getAccessControl().startLoginByCertificate(entity,
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
      throw new WrongPrivateKey("Erro ao descriptografar desafio.", e);
    }
    catch (AccessDenied e) {
      throw new OpenBusInternalException("Desafio enviado difere do esperado.",
        e);
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
      throw new CorruptedPrivateKey(
        "Erro ao interpretar bytes da chave privada", e);
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
          busid(), login.id, login.entity));
    fireRenewerThread();
  }

  /**
   * {@inheritDoc}
   * 
   * @throws ServiceFailure
   */
  @Override
  public LoginProcess startSharedAuth(OctetSeqHolder secret)
    throws ServiceFailure {
    EncryptedBlockHolder challenge = new EncryptedBlockHolder();
    LoginProcess process = this.access().startLoginBySharedAuth(challenge);
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
  public void loginBySharedAuth(LoginProcess process, byte[] secret)
    throws WrongSecret, AlreadyLoggedIn, ServiceFailure, InvalidLoginProcess {
    checkLoggedIn();
    this.manager.ignoreCurrentThread();

    this.bus.retrieveBusIdAndKey();

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
    if (this.login == null) {
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
      localLogout();
    }
    return true;
  }

  /**
   * Realiza o logout localmente.
   */
  void localLogout() {
    this.joinedChains.clear();
    stopRenewerThread();
    Connection conn = manager.getDispatcher(busid());
    if ((conn != null) && (conn.equals(this))) {
      manager.clearDispatcher(busid());
    }
    logger.info(String.format("Logout efetuado: id (%s) entidade (%s)",
      login.id, login.entity));
    this.login = null;
    this.getBus().clearBusIdAndKey();
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
      String loginid = any.extract_string();
      if (!this.login.id.equals(loginid)) {
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

  void setLegacyInfo(LegacyInfo legacy) {
    this.legacyBus = legacy;
  }

  boolean legacy() {
    return this.legacyBus != null;
  }

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
}
