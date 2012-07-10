package tecgraf.openbus;

import org.omg.CORBA.NO_PERMISSION;

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
   * 
   * @return <code>true</code> se a chamada que recebeu a indica��o que o login
   *         se tornou inv�lido deve ser refeita, ou <code>false</code> caso a
   *         exec��o de {@link NO_PERMISSION} deve ser lan�ada.
   */
  boolean invalidLogin(Connection conn, LoginInfo login);
}
