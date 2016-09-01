package tecgraf.openbus.core;

import java.util.concurrent.TimeUnit;

class OfferRegistryRetryContext extends InfiniteRetryContext {
  private final LocalOfferImpl offer;

  /**
   * Construtor
   *
   * @param delay tempo de espera entre tentativas
   */
  public OfferRegistryRetryContext(long delay, TimeUnit unit, LocalOfferImpl
    offer) {
    super(delay, unit);
    this.offer = offer;
  }

  @Override
  public void setLastException(Exception ex) {
    super.setLastException(ex);
    offer.error(ex);
  }
}
