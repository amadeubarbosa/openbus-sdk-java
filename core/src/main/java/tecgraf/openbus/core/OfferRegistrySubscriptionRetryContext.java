package tecgraf.openbus.core;

import tecgraf.openbus.retry.RetryContext;

import java.util.concurrent.TimeUnit;

class OfferRegistrySubscriptionRetryContext extends RetryContext {
  private final OfferRegistrySubscriptionImpl sub;

  /**
   * Construtor
   *
   * @param delay tempo de espera entre tentativas
   */
  public OfferRegistrySubscriptionRetryContext(long delay, TimeUnit unit,
    OfferRegistrySubscriptionImpl sub) {
    super(delay, unit);
    this.sub = sub;
  }

  @Override
  public void setLastException(Exception ex) {
    super.setLastException(ex);
    sub.error(ex);
  }
}
