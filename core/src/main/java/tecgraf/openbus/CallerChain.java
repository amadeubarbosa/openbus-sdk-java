package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Cadeia de chamadas oriundas de um barramento.
 * <p>
 * Coleção de informações dos logins que originaram chamadas em cadeia através
 * de um barramento. Cadeias de chamadas representam chamadas aninhadas dentro
 * do barramento e são úteis para que os sistemas que recebam essas chamadas
 * possam identificar se a chamada foi originada por entidades autorizadas ou
 * não.
 * 
 * @author Tecgraf
 */
public interface CallerChain {

  /**
   * Recupera o identificador do barramento através do qual essas chamadas foram
   * originadas.
   * 
   * @return Identificador do barramento.
   */
  String busid();

  /**
   * Recupera entidade para o qual a chamada estava destinada. Só é possível
   * fazer chamadas dentro dessa cadeia (através do método joinChain da
   * interface {@link OpenBusContext}) se a entidade da conexão corrente for o
   * mesmo do target.
   * <p>
   * No caso de conexões legadas, este campo conterá o nome da entidade da
   * conexão que atendeu (validou) a requisição. Todas as chamadas feitas como
   * parte de uma cadeia de uma chamada legada serão feitas utilizando apenas o
   * protocolo do OpenBus 1.5 (apenas com credenciais legadas) e portanto serão
   * recusadas por serviços que não aceitem chamadas legadas (OpenBus 1.5).
   * 
   * @return Indentificador da entidade para o qual a chamada estava destinada.
   */
  String target();

  /**
   * Recupera a lista de informações de login de todas as entidades que
   * originaram as chamadas nessa cadeia. Quando essa lista é vazia isso indica
   * que a chamada não está inclusa em outra cadeia de chamadas.
   * 
   * @return lista de logins.
   */
  LoginInfo[] originators();

  /**
   * Recupera a informação de login da entidade que realizou a última chamada da
   * cadeia.
   * 
   * @return a informação de login.
   */
  LoginInfo caller();
}
