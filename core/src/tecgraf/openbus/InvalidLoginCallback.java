package tecgraf.openbus;

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

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
   * Método a ser implementada pelo objeto de callback a ser chamado quando uma
   * notificação de login inválido é recebida.
   * 
   * @param conn Conexão que recebeu a notificação de login inválido.
   * @param login Informações do login que se tornou inválido.
   */
  void invalidLogin(Connection conn, LoginInfo login);
}
