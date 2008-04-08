/*
 * $Id$
 */
package openbus.common;

import openbusidl.acs.CredentialHolder;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.ORB;

/**
 * Gerenciador de conexões das entidades que se autenticam através de
 * usuário/senha.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class ClientConnectionManager extends ConnectionManager {
  /**
   * O usuário.
   */
  private String user;
  /**
   * A senha.
   */
  private String password;

  /**
   * Cria um gerenciador de conexões.
   * 
   * @param orb O ORB utilizado para obter o Serviço de Controle de Acesso.
   * @param host A máquina onde se encontra o Serviço de Controle de Acesso.
   * @param port A porta onde se encontra o Serviço de Controle de Acesso.
   * @param user O usuário.
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