/*
 * $Id$
 */
package openbus.common;

import openbusidl.acs.CredentialHolder;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.ORB;

/**
 * Gerenciador de conex�es das entidades que se autenticam atrav�s de
 * usu�rio/senha.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class ClientConnectionManager extends ConnectionManager {
  /**
   * O usu�rio.
   */
  private String user;
  /**
   * A senha.
   */
  private String password;

  /**
   * Cria um gerenciador de conex�es.
   * 
   * @param orb O ORB utilizado para obter o Servi�o de Controle de Acesso.
   * @param host A m�quina onde se encontra o Servi�o de Controle de Acesso.
   * @param port A porta onde se encontra o Servi�o de Controle de Acesso.
   * @param user O usu�rio.
   * @param password A senha.
   */
  public ClientConnectionManager(ORB orb, String host, int port, String user,
    String password) {
    super(orb, host, port);
    this.user = user;
    this.password = password;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean doLogin() {
    IAccessControlService acs =
      Utils.fetchAccessControlService(this.getORB(), this.getHost(), this
        .getPort());
    if (acs == null) {
      return false;
    }
    CredentialHolder credentialHolder = new CredentialHolder();
    IntHolder leaseHolder = new IntHolder();
    if (!acs.loginByPassword(this.user, this.password, credentialHolder,
      leaseHolder)) {
      return false;
    }
    this.setAccessControlService(acs);
    this.setCredential(credentialHolder.value);
    return true;
  }
}