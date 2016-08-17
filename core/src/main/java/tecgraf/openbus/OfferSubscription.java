package tecgraf.openbus;

/**
 * Representa uma inscrição de um observador de oferta de serviço. Essa
 * inscrição será mantida no barramento pelo registro de ofertas do qual se
 * originou, até que a aplicação remova-a ou realize um logout proposital.
 *
 * Os possíveis eventos gerados são definidos pela interface
 * {@link OfferObserver}.
 *
 * @author Tecgraf
 */
public interface OfferSubscription {
  /**
   * Recupera a referência para o observador inscrito pela aplicação.
   * 
   * @return o observador.
   */
  OfferObserver observer();

  /**
   * Recupera a referência para a oferta observada.
   * 
   * @return Uma referência para a oferta remota.
   */
  RemoteOffer offer();

  /**
   * Remove a incrição do observador, localmente. Uma outra thread será
   * encarregada de remover o observador do barramento.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará e se manterá interrompida.
   */
  void remove();
}