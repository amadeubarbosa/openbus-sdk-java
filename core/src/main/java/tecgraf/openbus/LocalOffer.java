package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;

import java.util.concurrent.TimeoutException;

/**
 * Representa uma oferta gerada por este processo. Essa oferta ser� mantida no
 * barramento pelo registro de ofertas do qual se originou, at� que a
 * aplica��o remova-a ou realize um logout proposital.
 *
 * @author Tecgraf
 */
public interface LocalOffer {
  /**
   * Fornece uma oferta remota relacionada ao pedido de registro que originou
   * este objeto. Caso algum erro esteja impedindo o registro, lan�ar� a
   * exce��o que corresponde ao �ltimo erro recebido.
   *
   * O SDK continuar� tentando realizar o registro periodicamente
   * independente do erro acusado, at� conseguir ou at� que o mesmo seja
   * removido pelo usu�rio atrav�s do m�todo {@link #remove()}.
   *
   * Caso a oferta n�o esteja registrada no barramento no momento da
   * chamada ou n�o haja um login v�lido, a chamada ficar� bloqueada at� que
   * essas condi��es sejam cumpridas. Caso seja interrompida, retornar�
   * <code>NULL</code> e se manter� interrompida.
   *
   * @throws ServiceFailure Caso haja algum erro inesperado no barramento ao
   * registrar a oferta.
   * @throws InvalidService O componente SCS fornecido n�o � v�lido, por n�o
   * apresentar facetas padr�o definidas pelo modelo de componetes SCS.
   * @throws InvalidProperties A lista de propriedades fornecida inclui
   * propriedades com nomes reservados (cujos nomes come�am com 'openbus.').
   * @throws UnauthorizedFacets O componente que implementa o servi�o
   * apresenta facetas com interfaces que n�o est�o autorizadas para a
   * entidade realizando o registro da oferta de servi�o.
   * @return A oferta remota, ou <code>NULL</code> caso o registro tenha sido
   * removido.
   */
  RemoteOffer remoteOffer() throws ServiceFailure, InvalidService,
    InvalidProperties, UnauthorizedFacets;

  /**
   * Fornece uma oferta remota relacionada ao pedido de registro que originou
   * este objeto. Caso algum erro esteja impedindo o registro, lan�ar� a
   * exce��o que corresponde ao �ltimo erro recebido.
   *
   * O SDK continuar� tentando realizar o registro periodicamente
   * independente do erro acusado, at� conseguir ou at� que o mesmo seja
   * removido pelo usu�rio atrav�s do m�todo {@link #remove()}.
   *
   * Caso a oferta n�o esteja registrada no barramento no momento da
   * chamada ou n�o haja um login v�lido, a chamada ficar� bloqueada at� que
   * essas condi��es sejam cumpridas ou o tempo se esgote. Caso seja
   * interrompida, retornar� <code>NULL</code> e se manter� interrompida.
   *
   * @param timeoutMillis O tempo m�ximo a aguardar pelo registro da oferta
   *                      remota, em milisegundos. O valor 0 neste par�metro
   *                      e no par�metro nanos faz com que a thread aguarde
   *                      eternamente, como na assinatura sem par�metros.
   * @throws ServiceFailure Caso haja algum erro inesperado no barramento ao
   * registrar a oferta.
   * @throws InvalidService O componente SCS fornecido n�o � v�lido, por n�o
   * apresentar facetas padr�o definidas pelo modelo de componetes SCS.
   * @throws InvalidProperties A lista de propriedades fornecida inclui
   * propriedades com nomes reservados (cujos nomes come�am com 'openbus.').
   * @throws UnauthorizedFacets O componente que implementa o servi�o
   * apresenta facetas com interfaces que n�o est�o autorizadas para a
   * entidade realizando o registro da oferta de servi�o.
   * @throws TimeoutException O tempo especificado se esgotou antes de a
   * oferta ser registrada.
   * @return A oferta remota, ou <code>NULL</code> caso o registro tenha sido
   * removido.
   */
  RemoteOffer remoteOffer(long timeoutMillis) throws ServiceFailure,
    InvalidService, InvalidProperties, UnauthorizedFacets, TimeoutException;

  /**
   * Solicita que a oferta n�o seja mais mantida no barramento. Caso a oferta
   * esteja registrada no momento da chamada, ela ser� removida do barramento
   * em uma outra thread.
   *
   * Ap�s a execu��o deste m�todo, os m�todos {@link #remoteOffer()} e
   * {@link #remoteOffer(long)} retornar�o <code>NULL</code>.
   */
  void remove();
}
