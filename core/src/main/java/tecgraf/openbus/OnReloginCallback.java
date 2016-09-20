package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Callback de login refeito.
 * <p>
 * Interface respons�vel por notifica��es de que o login foi refeito, para
 * que a aplica��o possa gerenciar seus pr�prios recursos dependentes do
 * login.
 *
 * Como o login pode ser refeito a qualquer momento, diversas
 * <i>callbacks</i> podem executar ao mesmo tempo. Portanto, a aplica��o
 * deve tratar quest�es de concorr�ncia de acordo.
 *
 * Recursos associados a funcionalidades b�sicas do barramento (logins,
 * ofertas, observadores de login, observadores de registro de oferta e
 * observadores de oferta) s�o gerenciados automaticamente pela biblioteca e
 * n�o precisam ser refeitos pela aplica��o.
 *
 * @author Tecgraf
 */
public interface OnReloginCallback {
  /**
   * Callback de login refeito.
   * <p>
   * Notifica que um login foi refeito. Caso este m�todo resulte em alguma
   * exce��o, o erro ser� capturado pela biblioteca e registrado no log.
   *
   * @param connection Conex�o que teve o login refeito.
   * @param oldLogin Login que se tornou inv�lido.
   */
  void onRelogin(Connection connection, LoginInfo oldLogin);
}
