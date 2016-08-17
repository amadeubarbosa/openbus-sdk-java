package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Interface de observa��o de eventos relacionados a logins.
 *
 * @author Tecgraf/PUC-Rio
 */
public interface LoginObserver {

  /**
   * Entidade teve o login finalizado junto ao barramento.
   * 
   * @param login informa��o do login finalizado
   */
  void entityLogout(LoginInfo login);

  /**
   * Ap�s o login desta aplica��o ser refeito, um ou mais logins
   * observados tornaram-se inexistentes.
   * @param loginIds informa��o dos logins finalizados
   */
  void nonExistentLogins(String[] loginIds);
}
