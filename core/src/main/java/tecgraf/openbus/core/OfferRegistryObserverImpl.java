package tecgraf.openbus.core;

import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistryObserverPOA;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.OfferRegistryObserver;
import tecgraf.openbus.RemoteOffer;

import java.util.logging.Level;
import java.util.logging.Logger;

class OfferRegistryObserverImpl extends OfferRegistryObserverPOA {
  public final OfferRegistryObserver observer;
  private final OfferRegistryImpl registry;
  private static final Logger logger = Logger.getLogger(
    OfferRegistryObserverImpl.class.getName());

  protected OfferRegistryObserverImpl(OfferRegistryObserver observer,
                                      OfferRegistryImpl registry) {
    this.observer = observer;
    this.registry = registry;
  }

  @Override
  public void offerRegistered(final ServiceOfferDesc offer) {
    Thread t = new Thread(() -> {
      RemoteOffer remote = new RemoteOfferImpl (registry, offer);
      String login = remote.owner().id;
      String entity = remote.owner().entity;
      try {
        observer.offerRegistered(remote);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Erro ao avisar um observador da aplicação " +
          "de que uma oferta do login " + login + " da entidade " + entity +
          " foi registrada.", e);
      }
    });
    t.start();
  }
}
