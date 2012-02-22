package tecgraf.openbus;

import java.security.interfaces.RSAPrivateKey;

import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_00.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_00.services.access_control.CertificateRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_00.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;

public interface Connection {
  void addObserver(ConnectionObserver observer);

  void removeObserver(ConnectionObserver observer);

  boolean isClosed();

  void close();

  Bus getBus();

  void loginByPassword(String entity, char[] password)
    throws AlreadyLoggedException, CryptographyException, InternalException,
    AccessDenied, WrongEncoding, ServiceFailure;

  void loginByCertificate(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedException, CryptographyException, InternalException,
    AccessDenied, MissingCertificate, WrongEncoding, ServiceFailure;

  void shareLogin(byte[] encodedlogin) throws CorruptedLoginException,
    InvalidLoginException, AlreadyLoggedException;

  LoginInfo getLogin();

  void logout() throws ServiceFailure;

  RSAPrivateKey getPrivateKey();

  void setAccessExpirationCallback(AccessExpirationCallback aec);

  void joinChain() throws InternalException;

  void joinChain(CallerChain chain);

  void exitChain();

  CallerChain getJoinedChain();

  AccessControl getAccessControl();

  LoginRegistry getLogins();

  CertificateRegistry getCertificates();

  OfferRegistry getOffers();
}