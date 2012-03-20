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
   * @return Lista de informações de login de todas as entidades que
   *         participaram dessa cadeia de chamadas, na ordem em que elas
   *         entraram na cadeia.
   */
  LoginInfo[] callers();
}
