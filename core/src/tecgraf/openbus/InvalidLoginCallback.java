package tecgraf.openbus;

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Callback de expira��o de login.
 * 
 * @author Tecgraf
 */
public interface InvalidLoginCallback {

  /**
   * Callback de expira��o de login.
   * 
   * @param conn a conex�o cujo login foi invalidado.
   * @param login o login inv�lido.
   * @param busid o identificador do barramento.
   */
  void invalidLogin(Connection conn, LoginInfo login, String busid);
}
