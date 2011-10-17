package tecgraf.openbus.defaultimpl;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.omg.CORBA.IntHolder;

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
import tecgraf.openbus.core.v2_00.OctetSeqHolder;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_00.services.access_control.CertificateRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.EntityLogin;
import tecgraf.openbus.core.v2_00.services.access_control.LoginByCertificate;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_00.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;

public final class ConnectionImpl implements Connection {
  private static final Logger logger = Logger.getLogger(ConnectionImpl.class
    .getName());

  private BusImpl bus;
  private X509Certificate certificate;
  private boolean closed;
  private Map<Thread, CallerChain> joinedChains;
  private Set<ConnectionObserver> observers;
  private LoginInfo login;

  ConnectionImpl(Bus bus) throws CryptographyException {
    this.bus = (BusImpl) bus;
    ByteArrayInputStream bais =
      new ByteArrayInputStream(this.bus.getAccessControl().certificate());
    try {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      this.certificate = (X509Certificate) cf.generateCertificate(bais);
    }
    catch (CertificateException e) {
      throw new CryptographyException(e);
    }
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
    throws AlreadyLoggedException, CryptographyException, AccessDenied,
    WrongEncoding, ServiceFailure {
    logger.info(String.format(
      "Iniciando a autenticação através de senha para a entidade %s", entity));
    byte[] encodedPassword =
      Cryptography.getInstance().encrypt(password, this.certificate);
    IntHolder validityHolder = new IntHolder();
    this.bus.getORB().ignoreCurrentThread();
    EntityLogin entityLogin;
    try {
      entityLogin =
        this.bus.getAccessControl().loginByPassword(entity, encodedPassword,
          validityHolder);
    }
    finally {
      this.bus.getORB().unignoreCurrentThread();
    }
    logger.info(String.format(
      "Autenticação através de senha efetuada com sucesso para a entidade %s",
      entity));
    this.login = new LoginInfo(entityLogin.id, entityLogin.entity);
  }

  @Override
  public void loginByCertificate(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedException, CryptographyException, AccessDenied,
    MissingCertificate, WrongEncoding, ServiceFailure {
    EntityLogin entityLogin;
    this.bus.getORB().ignoreCurrentThread();
    try {
      OctetSeqHolder challengeHolder = new OctetSeqHolder();
      LoginByCertificate loginByCertificate =
        this.bus.getAccessControl().startLoginByCertificate(entity,
          challengeHolder);
      byte[] decryptedChallenge =
        Cryptography.getInstance().decrypt(challengeHolder.value, privateKey);
      byte[] answer =
        Cryptography.getInstance().encrypt(decryptedChallenge, certificate);

      IntHolder validityHolder = new IntHolder();
      entityLogin = loginByCertificate.login(answer, validityHolder);
    }
    finally {
      this.bus.getORB().unignoreCurrentThread();
    }
    this.login = new LoginInfo(entityLogin.id, entityLogin.entity);
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
