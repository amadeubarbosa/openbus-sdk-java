package tecgraf.openbus;

import org.omg.CORBA.NO_PERMISSION;

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
   * 
   * @return <code>true</code> se a chamada que recebeu a indicação que o login
   *         se tornou inválido deve ser refeita, ou <code>false</code> caso a
   *         execção de {@link NO_PERMISSION} deve ser lançada.
   */
  boolean invalidLogin(Connection conn, LoginInfo login);
}
