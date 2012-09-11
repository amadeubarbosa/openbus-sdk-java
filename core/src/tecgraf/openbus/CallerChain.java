package tecgraf.openbus;

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

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
   * Recupera a informação de login a quem a cadeia se destina.
   * 
   * @return Informação de login.
   */
  LoginInfo target();

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
