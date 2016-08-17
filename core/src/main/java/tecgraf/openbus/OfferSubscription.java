package tecgraf.openbus;

/**
 * Representa uma inscri��o de um observador de oferta de servi�o. Essa
 * inscri��o ser� mantida no barramento pelo registro de ofertas do qual se
 * originou, at� que a aplica��o remova-a ou realize um logout proposital.
 *
 * Os poss�veis eventos gerados s�o definidos pela interface
 * {@link OfferObserver}.
 *
 * @author Tecgraf
 */
public interface OfferSubscription {
  /**
   * Recupera a refer�ncia para o observador inscrito pela aplica��o.
   * 
   * @return o observador.
   */
  OfferObserver observer();

  /**
   * Recupera a refer�ncia para a oferta observada.
   * 
   * @return Uma refer�ncia para a oferta remota.
   */
  RemoteOffer offer();

  /**
   * Remove a incri��o do observador, localmente. Uma outra thread ser�
   * encarregada de remover o observador do barramento.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� e se manter� interrompida.
   */
  void remove();
}