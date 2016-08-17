package tecgraf.openbus.core;

import tecgraf.openbus.OfferObserver;
import tecgraf.openbus.OfferSubscription;
import tecgraf.openbus.RemoteOffer;

class OfferSubscriptionImpl implements OfferSubscription {
  private final OfferRegistryImpl registry;
  private final OfferRegistryImpl.OfferSubscriptionContext context;
  private final RemoteOfferImpl offer;

  protected OfferSubscriptionImpl(OfferRegistryImpl registry,
                                  OfferRegistryImpl.OfferSubscriptionContext
                                    context, RemoteOfferImpl offer) {
    this.registry = registry;
    this.context = context;
    this.offer = offer;
  }

  @Override
  public OfferObserver observer() {
    return context.observer.observer;
  }

  @Override
  public RemoteOffer offer() {
    return offer;
  }

  @Override
  public void remove() {
    registry.removeOfferSubscription(context);
  }
}
