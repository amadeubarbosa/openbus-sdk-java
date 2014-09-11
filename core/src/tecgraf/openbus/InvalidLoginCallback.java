package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Callback de login inválido.
 * <p>
 * Interface a ser implementada pelo objeto de callback a ser chamado quando uma
 * notificação de login inválido é recebida.
 * 
 * @author Tecgraf
 */
public interface InvalidLoginCallback {

  /**
   * Callback de login inválido.
   * <p>
   * Método a ser implementado pelo objeto de callback a ser chamado quando uma
   * notificação de login inválido é recebida. Caso alguma exceção ocorra
   * durante a execução do método e não seja tratada, o erro será capturado pelo
   * interceptador e registrado no log.
   * 
   * @param conn Conexão que recebeu a notificação de login inválido.
   * @param login Informações do login que se tornou inválido.
   */
  void invalidLogin(Connection conn, LoginInfo login);
}
