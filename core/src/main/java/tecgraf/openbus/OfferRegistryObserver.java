package tecgraf.openbus;

/**
 * Interface de observação de eventos de registro de ofertas.
 *
 * @author Tecgraf/PUC-Rio
 */
public interface OfferRegistryObserver {

  /**
   * Uma oferta que atende aos critérios de observação foi registrada no
   * barramento.
   * 
   * @param offer a oferta registrada.
   */
  void offerRegistered(RemoteOffer offer);
}
