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
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        if (remote.offer() == null) {
          remote.offer(offer);
        }
        remote.updateProperties(RemoteOfferImpl.convertPropertiesToHashMap(offer
          .properties));
        observer.propertiesChanged(remote);
      }
    });
    t.start();
  }

  @Override
  public void removed(final ServiceOfferDesc offer) {
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        if (remote.offer() == null) {
          remote.offer(offer);
        }
        observer.removed(remote);
        registry.onOfferRemove(remote);
        remote.removed();
      }
    });
    t.start();
  }
}
