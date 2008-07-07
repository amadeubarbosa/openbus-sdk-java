/*
 * $Id$
 */
package openbus.common;

import openbus.common.exception.ACSUnavailableException;
import openbusidl.acs.Credential;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.ORB;

/**
 * Gerenciador de conexões das entidades que já possuem credencial pois já foram
 * autenticadas anteriormente.
 *
 * @author Tecgraf/PUC-Rio
 */
public final class CredentialConnectionManager extends ConnectionManager {
  /**
   * Cria um gerenciador de conexões.
   *
   * @param orb O ORB utilizado para obter o Serviço de Controle de Acesso.
   * @param host A máquina onde se encontra o Serviço de Controle de Acesso.
   * @param port A porta onde se encontra o Serviço de Controle de Acesso.
   * @param credential A credencial.
   */
  public CredentialConnectionManager(ORB orb, String host, int port,
    Credential credential) {
    super(orb, host, port);
    this.setCredential(credential);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean doLogin() throws ACSUnavailableException {
    IAccessControlService acs =
      Utils.fetchAccessControlService(this.getORB(), this.getHost(), this
        .getPort());
    if (acs == null) {
      throw new ACSUnavailableException(
        "Serviço de Controle de Acesso não disponível.");
    }
    this.setAccessControlService(acs);
    return true;
  }
}
