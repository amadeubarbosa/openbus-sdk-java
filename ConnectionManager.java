/*
 * $Id$
 */
package openbus.common;

import openbusidl.acs.Credential;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.ORB;

/**
 * Gerenciador de conex�es das entidades com o Servi�o de Controle de Acesso.
 * 
 * @author Tecgraf/PUC-Rio
 */
public abstract class ConnectionManager {
  /**
   * O ORB utilizado para obter o Servi�o de Controle de Acesso.
   */
  private ORB orb;
  /**
   * A m�quina onde se encontra o Servi�o de Controle de Acesso.
   */
  private String host;
  /**
   * A porta onde se encontra o Servi�o de Controle de Acesso.
   */
  private int port;
  /**
   * O Servi�o de Controle de Acesso.
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
   * Indica se a entidade est� conectada ao barramento.
   */
  private boolean connected;

  /**
   * Cria um gerenciador de conex�es.
   * 
   * @param orb O ORB utilizado para obter o Servi�o de Controle de Acesso.
   * @param host A m�quina onde se encontra o Servi�o de Controle de Acesso.
   * @param port A porta onde se encontra o Servi�o de Controle de Acesso.
   */
  protected ConnectionManager(ORB orb, String host, int port) {
    this.orb = orb;
    this.host = host;
    this.port = port;
  }

  /**
   * Conecta a entidade ao Servi�o de Controle de Acesso.
   * 
   * @return {@code true}, caso a conex�o seja realizada, ou {@code false},
   *         caso contr�rio.
   */
  public final boolean connect() {
    return this.connect(null);
  }

  /**
   * Conecta a entidade ao Servi�o de Controle de Acesso.
   * 
   * @param expiredCallback <i>Callback</i> usada para informar que a renova��o
   *        de um <i>lease</i> falhou.
   * 
   * @return {@code true}, caso a conex�o seja realizada, ou {@code false},
   *         caso contr�rio.
   */
  public final boolean connect(LeaseExpiredCallback expiredCallback) {
    if (!this.doLogin()) {
      return false;
    }
    this.connected = true;
    CredentialManager credentialManager = CredentialManager.getInstance();
    credentialManager.setORB(this.orb);
    credentialManager.setACS(this.accessControlService);
    credentialManager.setMemberCredential(this.credential);
    this.leaseRenewer =
      new LeaseRenewer(this.credential, this.accessControlService,
        expiredCallback);
    this.leaseRenewer.start();
    return true;
  }

  /**
   * Autentica a entidade no Servi�o de Controle de Acesso. Caso a autentica��o
   * seja realizada com sucesso deve definir a credencial e o servi�o de
   * controle de acesso atrav�s dos m�todos {@link #setCredential(Credential)} e
   * {@link #setAccessControlService(IAccessControlService)} respectivamente.
   * 
   * @return {@code true}, caso o <i>login</i> seja realizado, ou
   *         {@code false}, caso contr�rio.
   * 
   * @see #setAccessControlService(IAccessControlService)
   * @see #setCredential(Credential)
   */
  protected abstract boolean doLogin();

  /**
   * Desconecta a entidade do Servi�o de Controle de Acesso.
   * 
   * @return {@code true}, caso a desconex�o seja realizada, ou {@code false},
   *         caso contr�rio.
   */
  public final boolean disconnect() {
    if (!this.connected) {
      return false;
    }
    if (this.leaseRenewer != null) {
      this.leaseRenewer.finish();
      this.leaseRenewer = null;
    }
    this.accessControlService.logout(CredentialManager.getInstance()
      .getMemberCredential());
    CredentialManager.getInstance().invalidateMemberCredential();
    this.connected = false;
    return true;
  }

  /**
   * Verifica se a entidade est� conectada ao barramento.
   * 
   * @return {@code true}, caso a entidade esteja conectada ao barramento, ou
   *         {@code false}, caso contr�rio.
   */
  public boolean isConnected() {
    return this.connected;
  }

  /**
   * Obt�m o ORB utilizado para obter o Servi�o de Controle de Acesso.
   * 
   * @return O ORB utilizado para obter o Servi�o de Controle de Acesso.
   */
  protected final ORB getORB() {
    return this.orb;
  }

  /**
   * Obt�m a m�quina onde se encontra o Servi�o de Controle de Acesso.
   * 
   * @return A m�quina onde se encontra o Servi�o de Controle de Acesso.
   */
  protected final String getHost() {
    return this.host;
  }

  /**
   * Obt�m a porta onde se encontra o Servi�o de Controle de Acesso.
   * 
   * @return A porta onde se encontra o Servi�o de Controle de Acesso.
   */
  protected final int getPort() {
    return this.port;
  }

  /**
   * Obt�m o Servi�o de Controle de Acesso.
   * 
   * @return O Servi�o de Controle de Acesso.
   */
  public final IAccessControlService getAccessControlService() {
    return this.accessControlService;
  }

  /**
   * Define o Servi�o de Controle de Acesso.
   * 
   * @param accessControlService O Servi�o de Controle de Acesso.
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