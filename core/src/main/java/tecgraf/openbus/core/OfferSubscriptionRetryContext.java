package tecgraf.openbus.core;

import tecgraf.openbus.retry.RetryContext;

import java.util.concurrent.TimeUnit;

class OfferSubscriptionRetryContext extends RetryContext {
  private final OfferSubscriptionImpl sub;

  /**
   * Construtor
   *
   * @param delay tempo de espera entre tentativas
   */
  public OfferSubscriptionRetryContext(long delay, TimeUnit unit,
    OfferSubscriptionImpl sub) {
    super(delay, unit);
    this.sub = sub;
  }

  @Override
  public void setLastException(Throwable ex) {
    super.setLastException(ex);
    sub.error(ex);
  }
}
