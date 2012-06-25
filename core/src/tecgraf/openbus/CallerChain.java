package tecgraf.openbus;

import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

/**
 * Cadeia de chamadas oriundas de um barramento.
 * 
 * @author Tecgraf
 */
public interface CallerChain {

  /**
   * @return Barramento através do qual as chamadas foram originadas.
   */
  String busid();

  /**
   * Lista de informações de login de todas as entidades que realizaram chamadas
   * que originaram a cadeia de chamadas da qual essa chamada está inclusa.
   * Quando essa lista é vazia isso indica que a chamada não está inclusa numa
   * cadeia de chamadas.
   * 
   * @return lista de LoginInfo.
   */
  LoginInfo[] originators();

  /**
   * Informação de login da entidade que iniciou a chamada.
   * 
   * @return LoginInfo.
   */
  LoginInfo caller();
}
