package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Callback de login refeito.
 * <p>
 * Interface responsável por notificações de que o login foi refeito, para
 * que a aplicação possa gerenciar seus próprios recursos dependentes do
 * login.
 *
 * Como o login pode ser refeito a qualquer momento, diversas
 * <i>callbacks</i> podem executar ao mesmo tempo. Portanto, a aplicação
 * deve tratar questões de concorrência de acordo.
 *
 * Recursos associados a funcionalidades básicas do barramento (logins,
 * ofertas, observadores de login, observadores de registro de oferta e
 * observadores de oferta) são gerenciados automaticamente pela biblioteca e
 * não precisam ser refeitos pela aplicação.
 *
 * @author Tecgraf
 */
public interface OnReloginCallback {
  /**
   * Callback de login refeito.
   * <p>
   * Notifica que um login foi refeito. Caso este método resulte em alguma
   * exceção, o erro será capturado pela biblioteca e registrado no log.
   *
   * @param connection Conexão que teve o login refeito.
   * @param oldLogin Login que se tornou inválido.
   */
  void onRelogin(Connection connection, LoginInfo oldLogin);
}
