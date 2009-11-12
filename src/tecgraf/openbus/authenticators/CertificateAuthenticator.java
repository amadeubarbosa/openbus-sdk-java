/*
 * $Id$
 */
package tecgraf.openbus.authenticators;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.text.MessageFormat;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHolder;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.IntHolder;

import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.PKIException;
import tecgraf.openbus.util.Utils;

/**
 * Utilizado para efetuar a autentica��o de uma entidade junto ao Servi�o de
 * Controle de Acesso atrav�s de chave privada.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class CertificateAuthenticator implements Authenticator {
  /**
   * O nome da entidade.
   */
  private String entityName;
  /**
   * A chave privada da entidade.
   */
  private RSAPrivateKey privateKey;
  /**
   * O certificado digital do Servi�o de Controle de Acesso.
   */
  private X509Certificate acsCertificate;

  /**
   * Cria um autenticador que utiliza chave privada.
   * 
   * @param entityName O nome da entidade.
   * @param privateKey A chave privada da entidade.
   * @param acsCertificate O certificado digital do Servi�o de Controle de
   *        Acesso.
   */
  public CertificateAuthenticator(String entityName, RSAPrivateKey privateKey,
    X509Certificate acsCertificate) {
    this.entityName = entityName;
    this.privateKey = privateKey;
    this.acsCertificate = acsCertificate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Credential authenticate(IAccessControlService acs)
    throws ACSLoginFailureException, PKIException {
    byte[] challenge;
    challenge = acs.getChallenge(this.entityName);
    if (challenge.length == 0) {
      throw new ACSLoginFailureException(
        MessageFormat
          .format(
            "N�o foi poss�vel realizar a autentica��o no barramento. Provavelmente, a entidade {0} n�o est� cadastrada.",
            this.entityName));
    }
    byte[] answer;
    try {
      answer =
        Utils.generateAnswer(challenge, this.privateKey, this.acsCertificate);
    }
    catch (GeneralSecurityException e) {
      throw new PKIException(
        "Ocorreu um erro ao realizar a autentica��o no barramento. Verifique se a chave privada utilizada corresponde ao certificado digital cadastrado.",
        e);
    }

    CredentialHolder credentialHolder = new CredentialHolder();
    if (acs.loginByCertificate(this.entityName, answer, credentialHolder,
      new IntHolder())) {
      return credentialHolder.value;
    }
    return null;
  }
}
