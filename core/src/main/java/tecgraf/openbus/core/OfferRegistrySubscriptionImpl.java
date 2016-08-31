package tecgraf.openbus.core;

import com.google.common.collect.ArrayListMultimap;
import tecgraf.openbus.OfferRegistryObserver;
import tecgraf.openbus.OfferRegistrySubscription;

/**
 * Representa��o local de uma inscri��o de observa��o de registro de oferta
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
  public ArrayListMultimap<String, String> properties() {
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
