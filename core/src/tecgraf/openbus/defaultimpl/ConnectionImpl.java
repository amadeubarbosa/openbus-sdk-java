package tecgraf.openbus.defaultimpl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.IntHolder;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import tecgraf.openbus.AccessExpirationCallback;
import tecgraf.openbus.AlreadyLoggedException;
import tecgraf.openbus.Bus;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionObserver;
import tecgraf.openbus.CorruptedLoginException;
import tecgraf.openbus.CryptographyException;
import tecgraf.openbus.InternalException;
import tecgraf.openbus.InvalidLoginException;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.v2_00.EncryptedBlockHolder;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_00.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_00.services.access_control.CertificateRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.LoginAuthenticationInfo;
import tecgraf.openbus.core.v2_00.services.access_control.LoginAuthenticationInfoHelper;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_00.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_00.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;

public final class ConnectionImpl implements Connection {
  private static final Logger logger = Logger.getLogger(ConnectionImpl.class
    .getName());

  private BusImpl bus;
  private RSAPublicKey publicKey;
  private RSAPrivateKey privateKey;

  private boolean closed;
  private Map<Thread, CallerChain> joinedChains;
  private Set<ConnectionObserver> observers;
  private LoginInfo login;

  ConnectionImpl(Bus bus) throws CryptographyException {
    this.bus = (BusImpl) bus;
    KeyPair keyPair = Cryptography.getInstance().generateRSAKeyPair();
    this.publicKey = (RSAPublicKey) keyPair.getPublic();
    this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
    this.closed = false;
    this.joinedChains = new HashMap<Thread, CallerChain>();
    this.observers = new HashSet<ConnectionObserver>();
  }

  @Override
  public boolean isClosed() {
    return this.closed;
  }

  @Override
  public void close() {
    this.closed = true;
    Thread notificationThread = new Thread() {
      public void run() {
        for (ConnectionObserver observer : ConnectionImpl.this.observers) {
          observer.connectionClosed(ConnectionImpl.this);
        }
      };
    };
  }

  @Override
  public Bus getBus() {
    return this.bus;
  }

  @Override
  public void loginByPassword(String entity, char[] password)
    throws AlreadyLoggedException, CryptographyException, InternalException,
    AccessDenied, WrongEncoding, ServiceFailure {
    logger.info(String.format(
      "Iniciando a autenticação através de senha para a entidade %s", entity));

    CharBuffer passwordCharBuffer = CharBuffer.wrap(password);
    ByteBuffer passwordByteBuffer = OpenBus.CHARSET.encode(passwordCharBuffer);
    byte[] passwordByteArray = passwordByteBuffer.array();

    byte[] encryptedLoginAuthenticationInfo =
      this.generateEncryptedLoginAuthenticationInfo(passwordByteArray);
    IntHolder validityHolder = new IntHolder();
    String loginId;
    this.bus.getORB().ignoreCurrentThread();
    try {
      loginId =
        this.bus.getAccessControl().loginByPassword(entity,
          this.publicKey.getEncoded(), encryptedLoginAuthenticationInfo,
          validityHolder);
    }
    finally {
      this.bus.getORB().unignoreCurrentThread();
    }
    logger.info(String.format(
      "Autenticação através de senha efetuada com sucesso para a entidade %s",
      entity));
    this.login = new LoginInfo(loginId, entity);
  }

  private byte[] generateEncryptedLoginAuthenticationInfo(byte[] data)
    throws CryptographyException, InternalException {
    Cryptography crypto = Cryptography.getInstance();
    byte[] publicKeyHash = crypto.generateHash(this.publicKey.getEncoded());

    LoginAuthenticationInfo authenticationInfo =
      new LoginAuthenticationInfo(publicKeyHash, data);

    Any authenticationInfoAny = this.bus.getORB().getORB().create_any();
    LoginAuthenticationInfoHelper.insert(authenticationInfoAny,
      authenticationInfo);

    byte[] encodedLoginAuthenticationInfo;
    try {
      encodedLoginAuthenticationInfo =
        this.bus.getORB().getCodec().encode_value(authenticationInfoAny);
    }
    catch (InvalidTypeForEncoding e) {
      String message =
        "Falha inesperada ao codificar as informações de autenticação";
      logger.log(Level.SEVERE, message, e);
      throw new InternalException(message, e);
    }

    return crypto.encrypt(encodedLoginAuthenticationInfo, this.bus
      .getPublicKey());
  }

  @Override
  public void loginByCertificate(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedException, CryptographyException, InternalException,
    AccessDenied, MissingCertificate, WrongEncoding, ServiceFailure {
    String loginId;
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
      loginId =
        loginProcess.login(this.publicKey.getEncoded(),
          encryptedLoginAuthenticationInfo, validityHolder);
    }
    finally {
      this.bus.getORB().unignoreCurrentThread();
    }
    this.login = new LoginInfo(loginId, entity);
  }

  @Override
  public void shareLogin(byte[] encodedlogin) throws CorruptedLoginException,
    InvalidLoginException, AlreadyLoggedException {
    // TODO Auto-generated method stub

  }

  @Override
  public LoginInfo getLogin() {
    return this.login;
  }

  @Override
  public void logout() throws ServiceFailure {
    this.bus.getAccessControl().logout();
  }

  @Override
  public RSAPrivateKey getPrivateKey() {
    return this.privateKey;
  }

  @Override
  public void setAccessExpirationCallback(AccessExpirationCallback aec) {
    // TODO Auto-generated method stub
  }

  @Override
  public void joinChain() throws InternalException {
    CallerChain currentChain = this.bus.getORB().getCallerChain();
    if (currentChain == null) {
      return;
    }
    Thread currentThread = Thread.currentThread();
    this.joinedChains.put(currentThread, currentChain);
  }

  @Override
  public void joinChain(CallerChain chain) {
    Thread currentThread = Thread.currentThread();
    this.joinedChains.put(currentThread, chain);
  }

  @Override
  public void exitChain() {
    Thread currentThread = Thread.currentThread();
    this.joinedChains.remove(currentThread);
  }

  @Override
  public CallerChain getJoinedChain() {
    Thread currentThread = Thread.currentThread();
    return this.joinedChains.get(currentThread);
  }

  @Override
  public AccessControl getAccessControl() {
    return this.bus.getAccessControl();
  }

  @Override
  public LoginRegistry getLogins() {
    return this.bus.getLoginRegistry();
  }

  @Override
  public CertificateRegistry getCertificates() {
    return this.bus.getCertificateRegistry();
  }

  @Override
  public OfferRegistry getOffers() {
    return this.bus.getOfferRegistry();
  }

  public void addObserver(ConnectionObserver observer) {
    this.observers.add(observer);
  }

  public void removeObserver(ConnectionObserver observer) {
    this.observers.remove(observer);
  }
}
