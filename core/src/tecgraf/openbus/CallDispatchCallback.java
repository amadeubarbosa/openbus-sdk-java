package tecgraf.openbus;

import org.omg.CORBA.NO_PERMISSION;

import tecgraf.openbus.core.v2_0.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidRemoteCode;

/**
 * Callback de despacho de chamada.
 * <p>
 * Interface a ser implementada pelo objeto de callback a ser chamado quando uma
 * chamada proveniente de um barramento � recebida, que define a conex�o a ser
 * utilizada na valida��o dessa chamada.
 * 
 * @author Tecgraf
 */
public interface CallDispatchCallback {

  /**
   * Callback de login inv�lido.
   * <p>
   * M�todo a ser implementado pelo objeto de callback a ser chamado quando uma
   * chamada proveniente de um barramento � recebida. Esse m�todo � chamado para
   * determinar a conex�o a ser utilizada na valida��o de cada chamada recebida.
   * Se a conex�o informada n�o estiver conectada ao mesmo barramento indicado
   * pelo par�metro 'busid', a chamada provavelmente ser� recusada com um
   * {@link NO_PERMISSION}[{@link InvalidLoginCode}] pelo fato do login
   * provavelmente n�o ser v�lido no barramento da conex�o. Como resultado disso
   * o cliente da chamada poder� indicar que o servidor n�o est� implementado
   * corretamente e lan�ar a exce��o {@link NO_PERMISSION}[
   * {@link InvalidRemoteCode}]. Caso alguma exce��o ocorra durante a execu��o
   * do m�todo e n�o seja tratada, o erro ser� capturado pelo interceptador e
   * registrado no log.
   * 
   * @param context Gerenciador de contexto do ORB que recebeu a chamada.
   * @param busid Identifica��o do barramento atrav�s do qual a chamada foi
   *        feita.
   * @param loginId Informa��es do login que se tornou inv�lido.
   * @param object_id Idenficador opaco descrevendo o objeto sendo chamado.
   * @param operation Nome da opera��o sendo chamada.
   * 
   * @return Conex�o a ser utilizada para receber a chamada.
   */
  Connection dispatch(OpenBusContext context, String busid, String loginId,
    byte[] object_id, String operation);

}
