package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Callback de login inv�lido.
 * <p>
 * Interface a ser implementada pelo objeto de callback a ser chamado quando uma
 * notifica��o de login inv�lido � recebida.
 * 
 * @author Tecgraf
 */
public interface InvalidLoginCallback {

  /**
   * Callback de login inv�lido.
   * <p>
   * M�todo a ser implementado pelo objeto de callback a ser chamado quando uma
   * notifica��o de login inv�lido � recebida. Caso alguma exce��o ocorra
   * durante a execu��o do m�todo e n�o seja tratada, o erro ser� capturado pelo
   * interceptador e registrado no log.
   * 
   * @param conn Conex�o que recebeu a notifica��o de login inv�lido.
   * @param login Informa��es do login que se tornou inv�lido.
   */
  void invalidLogin(Connection conn, LoginInfo login);
}
