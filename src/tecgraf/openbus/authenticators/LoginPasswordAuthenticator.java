/*
 * $Id$
 */
package tecgraf.openbus.authenticators;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHolder;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.IntHolder;

/**
 * Utilizado para efetuar a autenticação de uma entidade junto ao Serviço de
 * Controle de Acesso através de um login e uma senha.
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
