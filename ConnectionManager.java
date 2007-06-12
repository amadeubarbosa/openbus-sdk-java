/*
 * $Id$
 */
package openbus.common;

import openbusidl.acs.Credential;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.ORB;

/**
 * Gerenciador de conexões das entidades com o Serviço de Controle de Acesso.
 * 
 * @author Tecgraf/PUC-Rio
 */
public abstract class ConnectionManager {
  /**
   * O ORB utilizado para obter o Serviço de Controle de Acesso.
   */
  private ORB orb;
  /**
   * A máquina onde se encontra o Serviço de Controle de Acesso.
   */
  private String host;
  /**
   * A porta onde se encontra o Serviço de Controle de Acesso.
   */
  private int port;
  /**
   * O Serviço de Controle de Acesso.
   */
  private IAccessControlService accessControlService;
  /**
   * A credencial da entidade.
   */
  private Credential credential;
  /**
   * O renovador do <i>lease</i>.
   */
  private LeaseRenewer leaseRenewer;

  /**
   * Cria um gerenciador de conexões.
   * 
   * @param orb O ORB utilizado para obter o Serviço de Controle de Acesso.
   * @param host A máquina onde se encontra o Serviço de Controle de Acesso.
   * @param port A porta onde se encontra o Serviço de Controle de Acesso.
   */
  protected ConnectionManager(ORB orb, String host, int port) {
    this.orb = orb;
    this.host = host;
    this.port = port;
  }

  /**
   * Conecta a entidade ao Serviço de Controle de Acesso.
   * 
   * @return {@code true}, caso a conexão seja realizada, ou {@code false},
   *         caso contrário.
   */
  public final boolean connect() {
    if (!this.doLogin()) {
      return false;
    }
    CredentialManager credentialManager = CredentialManager.getInstance();
    credentialManager.setORB(this.orb);
    credentialManager.setACS(this.accessControlService);
    credentialManager.setMemberCredential(credential);
    this.leaseRenewer = new LeaseRenewer(this.credential,
      this.accessControlService);
    this.leaseRenewer.start();
    return true;
  }

  /**
   * Autentica a entidade no Serviço de Controle de Acesso. Caso a autenticação
   * seja realizada com sucesso deve definir a credencial e o serviço de
   * controle de acesso através dos métodos {@link #setCredential(Credential)} e
   * {@link #setAccessControlService(IAccessControlService)} respectivamente.
   * 
   * @return {@code true}, caso o <i>login</i> seja realizado, ou
   *         {@code false}, caso contrário.
   * 
   * @see #setAccessControlService(IAccessControlService)
   * @see #setCredential(Credential)
   */
  protected abstract boolean doLogin();

  /**
   * Desconecta a entidade do Serviço de Controle de Acesso.
   * 
   * @return {@code true}, caso a desconexão seja realizada, ou {@code false},
   *         caso contrário.
   */
  public final boolean disconnect() {
    if (this.leaseRenewer != null) {
      this.leaseRenewer.finish();
      this.leaseRenewer = null;
    }
    accessControlService.logout(CredentialManager.getInstance()
      .getMemberCredential());
    CredentialManager.getInstance().invalidateMemberCredential();
    return true;
  }

  /**
   * Obtém o ORB utilizado para obter o Serviço de Controle de Acesso.
   * 
   * @return O ORB utilizado para obter o Serviço de Controle de Acesso.
   */
  protected final ORB getORB() {
    return this.orb;
  }

  /**
   * Obtém a máquina onde se encontra o Serviço de Controle de Acesso.
   * 
   * @return A máquina onde se encontra o Serviço de Controle de Acesso.
   */
  protected final String getHost() {
    return this.host;
  }

  /**
   * Obtém a porta onde se encontra o Serviço de Controle de Acesso.
   * 
   * @return A porta onde se encontra o Serviço de Controle de Acesso.
   */
  protected final int getPort() {
    return this.port;
  }

  /**
   * Obtém o Serviço de Controle de Acesso.
   * 
   * @return O Serviço de Controle de Acesso.
   */
  public final IAccessControlService getAccessControlService() {
    return this.accessControlService;
  }

  /**
   * Define o Serviço de Controle de Acesso.
   * 
   * @param accessControlService O Serviço de Controle de Acesso.
   */
  protected final void setAccessControlService(
    IAccessControlService accessControlService) {
    this.accessControlService = accessControlService;
  }

  /**
   * Define a credencial da entidade.
   * 
   * @param credential A credencial da entidade.
   */
  protected final void setCredential(Credential credential) {
    this.credential = credential;
  }
}