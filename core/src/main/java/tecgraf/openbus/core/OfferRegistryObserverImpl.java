package tecgraf.openbus.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistryObserverPOA;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.OfferRegistryObserver;
import tecgraf.openbus.RemoteOffer;

class OfferRegistryObserverImpl extends OfferRegistryObserverPOA {
  public final OfferRegistryObserver observer;
  private final OfferRegistryImpl registry;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected OfferRegistryObserverImpl(OfferRegistryObserver observer,
                                      OfferRegistryImpl registry) {
    this.observer = observer;
    this.registry = registry;
  }

  @Override
  public void offerRegistered(final ServiceOfferDesc offer) {
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        RemoteOffer remote = new RemoteOfferImpl (registry, offer);
        String login = remote.owner().id;
        String entity = remote.owner().entity;
        try {
          observer.offerRegistered(remote);
        } catch (Exception e) {
          logger.error("Erro ao avisar um observador da aplicação de que " +
            "uma oferta do login " + login + " da entidade " + entity + " " +
            "foi registrada.", e);
        }
      }
    });
    t.start();
  }
}
