/*
 * $Id$
 */
package tecgraf.openbus.authenticators;

import org.omg.CORBA.IntHolder;

import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.CredentialHolder;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;

/**
 * Utilizado para efetuar a autentica��o de uma entidade junto ao Servi�o de
 * Controle de Acesso atrav�s de um login e uma senha.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class LoginPasswordAuthenticator implements Authenticator {
  /**
   * O nome da entidade (login) a ser autenticada.
   */
  private String name;
  /**
   * A senha da entidade.
   */
  private String password;

  /**
   * Cria um autenticador que utiliza login e senha.
   * 
   * @param name O login.
   * @param password A senha.
   */
  public LoginPasswordAuthenticator(String name, String password) {
    this.name = name;
    this.password = password;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Credential authenticate(IAccessControlService acs) {
    CredentialHolder credentialHolder = new CredentialHolder();
    if (acs.loginByPassword(this.name, this.password, credentialHolder,
      new IntHolder())) {
      return credentialHolder.value;
    }
    return null;
  }

}
