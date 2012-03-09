package tecgraf.openbus;

import java.security.interfaces.RSAPrivateKey;

import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_00.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_00.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.AlreadyLoggedException;
import tecgraf.openbus.exception.CorruptedLoginException;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.InternalException;
import tecgraf.openbus.exception.InvalidLoginException;

public interface Connection {
  void addObserver(ConnectionObserver observer);

  void removeObserver(ConnectionObserver observer);

  boolean isClosed();

  void close();

  public Bus getBus();

  /**
   * Autentica uma entidade através de uma senha.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha
   * @return A informação do login.
   * @throws AlreadyLoggedException
   * @throws CryptographyException
   * @throws InternalException
   * @throws AccessDenied
   * @throws WrongEncoding
   * @throws ServiceFailure
   */
  public LoginInfo loginByPassword(String entity, char[] password)
    throws AlreadyLoggedException, CryptographyException, InternalException,
    AccessDenied, WrongEncoding, ServiceFailure;

  /**
   * Autentica uma entidade através de um certificado.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey A chave privada da entidade.
   * @return A informação do login.
   * @throws AlreadyLoggedException
   * @throws CryptographyException
   * @throws InternalException
   * @throws AccessDenied
   * @throws MissingCertificate
   * @throws WrongEncoding
   * @throws ServiceFailure
   */
  public LoginInfo loginByCertificate(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedException, CryptographyException, InternalException,
    AccessDenied, MissingCertificate, WrongEncoding, ServiceFailure;

  public LoginProcess shareLogin(byte[] encodedlogin)
    throws CorruptedLoginException, InvalidLoginException,
    AlreadyLoggedException;

  /**
   * Obtém a informação do login desta conexão.
   * 
   * @return A informação do login.
   */
  public LoginInfo getLogin();

  /**
   * Encerra o login.
   * 
   * @throws ServiceFailure
   */
  public void logout() throws ServiceFailure;

  /**
   * @return
   */
  RSAPrivateKey getPrivateKey();

  void setAccessExpirationCallback(AccessExpirationCallback aec);

  public void joinChain() throws InternalException;

  public void joinChain(CallerChain chain);

  public void exitChain();

  public CallerChain getJoinedChain();

  public OfferRegistry getOffers();
}