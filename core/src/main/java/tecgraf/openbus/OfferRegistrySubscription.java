package tecgraf.openbus;

import com.google.common.collect.ArrayListMultimap;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;

import java.util.concurrent.TimeoutException;

/**
 * Representa uma inscrição de um observador de registro de oferta de serviço.
 * Essa inscrição será mantida no barramento pelo registro de ofertas do qual se
 * originou, até que a aplicação remova-a ou realize um logout proposital.
 *
 * Os possíveis eventos gerados são definidos pela interface
 * {@link OfferRegistryObserver}.
 *
 * @author Tecgraf
 */
public interface OfferRegistrySubscription {
  /**
   * Indica se a subscrição foi bem-sucedida ou não. Caso algum erro a esteja
   * impedindo, lançará a exceção que corresponde ao último erro recebido.
   *
   * O SDK continuará tentando realizar a subscrição periodicamente
   * independente do erro acusado, até conseguir ou até que a mesma seja
   * removida pelo usuário através do método {@link #remove()}.
   *
   * Caso a subscrição não esteja cadastrada no barramento no momento da
   * chamada ou não haja um login válido, a chamada ficará bloqueada até que
   * essas condições sejam cumpridas. Caso seja interrompida, retornará
   * false e se manterá interrompida.
   *
   * @throws ServiceFailure Caso haja algum erro inesperado no barramento ao
   * realizar a subscrição.
   * @return True caso a subscrição esteja ativa, false caso tenha sido
   * cancelada pelo usuário através do método {@link #remove()}.
   */
  boolean subscribed() throws ServiceFailure;

  /**
   * Indica se a subscrição foi bem-sucedida ou não. Caso algum erro a esteja
   * impedindo, lançará a exceção que corresponde ao último erro recebido.
   *
   * O SDK continuará tentando realizar a subscrição periodicamente
   * independente do erro acusado, até conseguir ou até que a mesma seja
   * removida pelo usuário através do método {@link #remove()}.
   *
   * Caso a subscrição não esteja cadastrada no barramento no momento da
   * chamada ou não haja um login válido, a chamada ficará bloqueada até que
   * essas condições sejam cumpridas ou o tempo se esgote. Caso seja
   * interrompida, retornará false e se manterá interrompida.
   *
   * @throws ServiceFailure Caso haja algum erro inesperado no barramento ao
   * realizar a subscrição.
   * @throws TimeoutException O tempo especificado se esgotou antes de a
   * subscrição ser completada.
   * @return True caso a subscrição esteja ativa, false caso tenha sido
   * cancelada pelo usuário através do método {@link #remove()}.
   */
  boolean subscribed(long timeoutMillis) throws ServiceFailure,
    TimeoutException;

  /**
   * Recupera a referência para o observador inscrito pela aplicação.
   * 
   * @return o observador.
   */
  OfferRegistryObserver observer();

  /**
   * Recupera a lista de propriedades de oferta nas quais o observador está
   * interessado.
   * 
   * @return a lista de propriedades.
   */
  ArrayListMultimap<String, String> properties();

  /**
   * Remove a incrição do observador, localmente. Uma outra thread será
   * encarregada de remover o observador do barramento.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará e se manterá interrompida.
   *
   * Após a execução deste método, os métodos {@link #subscribed()} e
   * {@link #subscribed(long)} retornarão <code>NULL</code>.
   */
  void remove();
}