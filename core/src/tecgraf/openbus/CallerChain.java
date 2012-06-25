package tecgraf.openbus;

import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

/**
 * Cadeia de chamadas oriundas de um barramento.
 * 
 * @author Tecgraf
 */
public interface CallerChain {

  /**
   * @return Barramento atrav�s do qual as chamadas foram originadas.
   */
  String busid();

  /**
   * Lista de informa��es de login de todas as entidades que realizaram chamadas
   * que originaram a cadeia de chamadas da qual essa chamada est� inclusa.
   * Quando essa lista � vazia isso indica que a chamada n�o est� inclusa numa
   * cadeia de chamadas.
   * 
   * @return lista de LoginInfo.
   */
  LoginInfo[] originators();

  /**
   * Informa��o de login da entidade que iniciou a chamada.
   * 
   * @return LoginInfo.
   */
  LoginInfo caller();
}
