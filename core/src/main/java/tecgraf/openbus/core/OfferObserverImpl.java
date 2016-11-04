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
    // define subscri��o como cancelada antes de chamar observer.removed()
    // (c�digo da aplica��o, pode demorar) apenas como dica, para n�o
    // arriscar de a aplica��o demorar a perceber que isso ocorreu atrav�s
    // do m�todo subscribed(). A chamada registry.onOfferRemove posterior
    // j� cuidaria de cancelar e remover a subscri��o.
    registry.onOfferRemove(remote, true);
    Thread t = new Thread(() -> {
      observer.removed(remote);
      registry.onOfferRemove(remote, false);
      remote.removed();
    });
    t.start();
  }
}
