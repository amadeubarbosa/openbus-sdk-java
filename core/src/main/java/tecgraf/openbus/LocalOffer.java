package tecgraf.openbus;

import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;

import java.util.concurrent.TimeoutException;

/**
 * Representa uma oferta gerada por este processo. Essa oferta será mantida no
 * barramento pelo registro de ofertas do qual se originou, até que a
 * aplicação remova-a ou realize um logout proposital.
 *
 * @author Tecgraf
 */
public interface LocalOffer {
  /**
   * Fornece uma oferta remota relacionada ao pedido de registro que originou
   * este objeto. Caso algum erro esteja impedindo o registro, lançará a
   * exceção que corresponde ao último erro recebido.
   *
   * O SDK continuará tentando realizar o registro periodicamente
   * independente do erro acusado, até conseguir ou até que o mesmo seja
   * removido pelo usuário através do método {@link #remove()}.
   *
   * Caso a oferta não esteja registrada no barramento no momento da
   * chamada ou não haja um login válido, a chamada ficará bloqueada até que
   * essas condições sejam cumpridas. Caso seja interrompida, retornará
   * <code>NULL</code> e se manterá interrompida.
   *
   * @throws ServiceFailure Caso haja algum erro inesperado no barramento ao
   * registrar a oferta.
   * @throws InvalidService O componente SCS fornecido não é válido, por não
   * apresentar facetas padrão definidas pelo modelo de componetes SCS.
   * @throws InvalidProperties A lista de propriedades fornecida inclui
   * propriedades com nomes reservados (cujos nomes começam com 'openbus.').
   * @throws UnauthorizedFacets O componente que implementa o serviço
   * apresenta facetas com interfaces que não estão autorizadas para a
   * entidade realizando o registro da oferta de serviço.
   * @return A oferta remota, ou <code>NULL</code> caso o registro tenha sido
   * removido.
   */
  RemoteOffer remoteOffer() throws ServiceFailure, InvalidService,
    InvalidProperties, UnauthorizedFacets;

  /**
   * Fornece uma oferta remota relacionada ao pedido de registro que originou
   * este objeto. Caso algum erro esteja impedindo o registro, lançará a
   * exceção que corresponde ao último erro recebido.
   *
   * O SDK continuará tentando realizar o registro periodicamente
   * independente do erro acusado, até conseguir ou até que o mesmo seja
   * removido pelo usuário através do método {@link #remove()}.
   *
   * Caso a oferta não esteja registrada no barramento no momento da
   * chamada ou não haja um login válido, a chamada ficará bloqueada até que
   * essas condições sejam cumpridas ou o tempo se esgote. Caso seja
   * interrompida, retornará <code>NULL</code> e se manterá interrompida.
   *
   * @param timeoutMillis O tempo máximo a aguardar pelo registro da oferta
   *                      remota, em milisegundos. O valor 0 neste parâmetro
   *                      e no parâmetro nanos faz com que a thread aguarde
   *                      eternamente, como na assinatura sem parâmetros.
   * @throws ServiceFailure Caso haja algum erro inesperado no barramento ao
   * registrar a oferta.
   * @throws InvalidService O componente SCS fornecido não é válido, por não
   * apresentar facetas padrão definidas pelo modelo de componetes SCS.
   * @throws InvalidProperties A lista de propriedades fornecida inclui
   * propriedades com nomes reservados (cujos nomes começam com 'openbus.').
   * @throws UnauthorizedFacets O componente que implementa o serviço
   * apresenta facetas com interfaces que não estão autorizadas para a
   * entidade realizando o registro da oferta de serviço.
   * @throws TimeoutException O tempo especificado se esgotou antes de a
   * oferta ser registrada.
   * @return A oferta remota, ou <code>NULL</code> caso o registro tenha sido
   * removido.
   */
  RemoteOffer remoteOffer(long timeoutMillis) throws ServiceFailure,
    InvalidService, InvalidProperties, UnauthorizedFacets, TimeoutException;

  /**
   * Solicita que a oferta não seja mais mantida no barramento. Caso a oferta
   * esteja registrada no momento da chamada, ela será removida do barramento
   * em uma outra thread.
   *
   * Após a execução deste método, os métodos {@link #remoteOffer()} e
   * {@link #remoteOffer(long)} retornarão <code>NULL</code>.
   */
  void remove();
}
