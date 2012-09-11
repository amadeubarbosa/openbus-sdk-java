package tecgraf.openbus;

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Cadeia de chamadas oriundas de um barramento.
 * <p>
 * Cole��o de informa��es dos logins que originaram chamadas em cadeia atrav�s
 * de um barramento. Cadeias de chamadas representam chamadas aninhadas dentro
 * do barramento e s�o �teis para que os sistemas que recebam essas chamadas
 * possam identificar se a chamada foi originada por entidades autorizadas ou
 * n�o.
 * 
 * @author Tecgraf
 */
public interface CallerChain {

  /**
   * Recupera o identificador do barramento atrav�s do qual essas chamadas foram
   * originadas.
   * 
   * @return Identificador do barramento.
   */
  String busid();

  /**
   * Recupera a informa��o de login a quem a cadeia se destina.
   * 
   * @return Informa��o de login.
   */
  LoginInfo target();

  /**
   * Recupera a lista de informa��es de login de todas as entidades que
   * originaram as chamadas nessa cadeia. Quando essa lista � vazia isso indica
   * que a chamada n�o est� inclusa em outra cadeia de chamadas.
   * 
   * @return lista de logins.
   */
  LoginInfo[] originators();

  /**
   * Recupera a informa��o de login da entidade que realizou a �ltima chamada da
   * cadeia.
   * 
   * @return a informa��o de login.
   */
  LoginInfo caller();
}
