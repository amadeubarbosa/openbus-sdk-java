package tecgraf.openbus.assistant;

import scs.core.IComponent;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;

/**
 * Callback de notifica��o de falhas capturadas pelo assistente.
 * <p>
 * Interface a ser implementada pelo objeto de callback a ser chamado quando o
 * assistente captura um erro durante suas tarefas, tais como login, registro ou
 * busca de ofertas de servi�o.
 * 
 * @author Tecgraf
 */
public interface OnFailureCallback {

  /**
   * Callback de notifica��o de falhas durante processo de login.
   * 
   * @param assistant assistant Assitente que chama a callback.
   * @param except Objeto que descreve a falha ocorrida.
   */
  void onLoginFailure(Assistant assistant, Exception except);

  /**
   * Callback de notifica��o de falhas durante processo de login.
   * 
   * @param assistant assistant Assitente que chama a callback.
   * @param component Componente sendo registrado.
   * @param properties Lista de propriedades com que o servi�o deveria ter sido
   *        registrado.
   * @param except Objeto que descreve a falha ocorrida.
   */
  void onRegisterFailure(Assistant assistant, IComponent component,
    ServiceProperty[] properties, Exception except);

  /**
   * Callback de notifica��o de falhas durante processo de login.
   * 
   * @param assistant assistant Assitente que chama a callback.
   * @param except Objeto que descreve a falha ocorrida.
   */
  void onFindFailure(Assistant assistant, Exception except);

}