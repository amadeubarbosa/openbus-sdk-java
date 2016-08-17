package tecgraf.openbus.core;

import tecgraf.openbus.OfferRegistryObserver;
import tecgraf.openbus.OfferRegistrySubscription;

import java.util.Map;

/**
 * Representação local de uma inscrição de observação de registro de oferta
 *
 * @author Tecgraf
 */
class OfferRegistrySubscriptionImpl implements OfferRegistrySubscription {
  private final OfferRegistryImpl registry;
  private final OfferRegistryImpl.OfferRegistrySubscriptionContext context;

  protected OfferRegistrySubscriptionImpl(OfferRegistryImpl registry,
    OfferRegistryImpl.OfferRegistrySubscriptionContext context) {
    this.registry = registry;
    this.context = context;
  }

  @Override
  public Map<String, String> properties() {
    return context.properties;
  }

  @Override
  public void remove() {
    registry.removeRegistrySubscription(context);
  }

  @Override
  public OfferRegistryObserver observer() {
    return context.observer.observer;
  }
}
