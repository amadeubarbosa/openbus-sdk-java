package tecgraf.openbus;

import com.google.common.collect.ArrayListMultimap;

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
   */
  void remove();
}