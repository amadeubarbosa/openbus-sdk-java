package tecgraf.openbus;

import com.google.common.collect.ArrayListMultimap;

/**
 * Representa uma inscri��o de um observador de registro de oferta de servi�o.
 * Essa inscri��o ser� mantida no barramento pelo registro de ofertas do qual se
 * originou, at� que a aplica��o remova-a ou realize um logout proposital.
 *
 * Os poss�veis eventos gerados s�o definidos pela interface
 * {@link OfferRegistryObserver}.
 *
 * @author Tecgraf
 */
public interface OfferRegistrySubscription {

  /**
   * Recupera a refer�ncia para o observador inscrito pela aplica��o.
   * 
   * @return o observador.
   */
  OfferRegistryObserver observer();

  /**
   * Recupera a lista de propriedades de oferta nas quais o observador est�
   * interessado.
   * 
   * @return a lista de propriedades.
   */
  ArrayListMultimap<String, String> properties();

  /**
   * Remove a incri��o do observador, localmente. Uma outra thread ser�
   * encarregada de remover o observador do barramento.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� e se manter� interrompida.
   */
  void remove();
}