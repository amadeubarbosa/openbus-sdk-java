package tecgraf.openbus;

import org.omg.CORBA.NO_PERMISSION;

import tecgraf.openbus.core.v2_0.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidRemoteCode;

/**
 * Callback de despacho de chamada.
 * <p>
 * Interface a ser implementada pelo objeto de callback a ser chamado quando uma
 * chamada proveniente de um barramento é recebida, que define a conexão a ser
 * utilizada na validação dessa chamada.
 * 
 * @author Tecgraf
 */
public interface CallDispatchCallback {

  /**
   * Callback de login inválido.
   * <p>
   * Método a ser implementado pelo objeto de callback a ser chamado quando uma
   * chamada proveniente de um barramento é recebida. Esse método é chamado para
   * determinar a conexão a ser utilizada na validação de cada chamada recebida.
   * Se a conexão informada não estiver conectada ao mesmo barramento indicado
   * pelo parâmetro 'busid', a chamada provavelmente será recusada com um
   * {@link NO_PERMISSION}[{@link InvalidLoginCode}] pelo fato do login
   * provavelmente não ser válido no barramento da conexão. Como resultado disso
   * o cliente da chamada poderá indicar que o servidor não está implementado
   * corretamente e lançar a exceção {@link NO_PERMISSION}[
   * {@link InvalidRemoteCode}]. Caso alguma exceção ocorra durante a execução
   * do método e não seja tratada, o erro será capturado pelo interceptador e
   * registrado no log.
   * 
   * @param context Gerenciador de contexto do ORB que recebeu a chamada.
   * @param busid Identificação do barramento através do qual a chamada foi
   *        feita.
   * @param loginId Informações do login que se tornou inválido.
   * @param object_id Idenficador opaco descrevendo o objeto sendo chamado.
   * @param operation Nome da operação sendo chamada.
   * 
   * @return Conexão a ser utilizada para receber a chamada.
   */
  Connection dispatch(OpenBusContext context, String busid, String loginId,
    byte[] object_id, String operation);

}
