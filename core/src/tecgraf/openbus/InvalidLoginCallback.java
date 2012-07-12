package tecgraf.openbus;

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Callback de expiração de login.
 * 
 * @author Tecgraf
 */
public interface InvalidLoginCallback {

  /**
   * Callback de expiração de login.
   * 
   * @param conn a conexão cujo login foi invalidado.
   * @param login o login inválido.
   * @param busid o identificador do barramento.
   */
  void invalidLogin(Connection conn, LoginInfo login, String busid);
}
