package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Interface de observação de eventos relacionados a logins.
 *
 * @author Tecgraf/PUC-Rio
 */
public interface LoginObserver {

  /**
   * Entidade teve o login finalizado junto ao barramento.
   * 
   * @param login informação do login finalizado
   */
  void entityLogout(LoginInfo login);

  /**
   * Após o login desta aplicação ser refeito, um ou mais logins
   * observados tornaram-se inexistentes.
   * @param loginIds informação dos logins finalizados
   */
  void nonExistentLogins(String[] loginIds);
}
