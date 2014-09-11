package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

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
   * Recupera entidade para o qual a chamada estava destinada. S� � poss�vel
   * fazer chamadas dentro dessa cadeia (atrav�s do m�todo joinChain da
   * interface {@link OpenBusContext}) se a entidade da conex�o corrente for o
   * mesmo do target.
   * <p>
   * No caso de conex�es legadas, este campo conter� o nome da entidade da
   * conex�o que atendeu (validou) a requisi��o. Todas as chamadas feitas como
   * parte de uma cadeia de uma chamada legada ser�o feitas utilizando apenas o
   * protocolo do OpenBus 1.5 (apenas com credenciais legadas) e portanto ser�o
   * recusadas por servi�os que n�o aceitem chamadas legadas (OpenBus 1.5).
   * 
   * @return Indentificador da entidade para o qual a chamada estava destinada.
   */
  String target();

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
