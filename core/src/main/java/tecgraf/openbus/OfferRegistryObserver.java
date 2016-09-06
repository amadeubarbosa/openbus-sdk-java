package tecgraf.openbus;

/**
 * Interface de observa��o de eventos de registro de ofertas.
 *
 * @author Tecgraf/PUC-Rio
 */
public interface OfferRegistryObserver {

  /**
   * Uma oferta que atende aos crit�rios de observa��o foi registrada no
   * barramento.
   * 
   * @param offer a oferta registrada.
   */
  void offerRegistered(RemoteOffer offer);
}
