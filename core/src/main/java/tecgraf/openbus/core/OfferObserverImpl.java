package tecgraf.openbus.core;

import tecgraf.openbus.core.v2_1.services.offer_registry.OfferObserverPOA;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.OfferObserver;

class OfferObserverImpl extends OfferObserverPOA {
  public final OfferObserver observer;
  private final OfferRegistryImpl registry;
  private final RemoteOfferImpl remote;

  protected OfferObserverImpl (OfferRegistryImpl registry, OfferObserver
    observer, RemoteOfferImpl remote) {
    this.registry = registry;
    this.observer = observer;
    this.remote = remote;
  }

  @Override
  public void propertiesChanged(final ServiceOfferDesc offer) {
    Thread t = new Thread(() -> {
      if (remote.offer() == null) {
        remote.offer(offer);
      }
      remote.updateProperties(RemoteOfferImpl.convertPropertiesToHashMap(offer
        .properties));
      observer.propertiesChanged(remote);
    });
    t.start();
  }

  @Override
  public void removed(final ServiceOfferDesc offer) {
    if (remote.offer() == null) {
      remote.offer(offer);
    }
    // define subscrição como cancelada antes de chamar observer.removed()
    // (código da aplicação, pode demorar) apenas como dica, para não
    // arriscar de a aplicação demorar a perceber que isso ocorreu através
    // do método subscribed(). A chamada registry.onOfferRemove posterior
    // já cuidaria de cancelar e remover a subscrição.
    registry.onOfferRemove(remote, true);
    Thread t = new Thread(() -> {
      observer.removed(remote);
      registry.onOfferRemove(remote, false);
      remote.removed();
    });
    t.start();
  }
}
