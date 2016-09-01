package tecgraf.openbus.core;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tecgraf.openbus.OfferRegistryObserver;
import tecgraf.openbus.OfferRegistrySubscription;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistryObserverSubscription;
import tecgraf.openbus.exception.OpenBusInternalException;

import java.util.concurrent.TimeoutException;

/**
 * Representação local de uma inscrição de observação de registro de oferta
 *
 * @author Tecgraf
 */
class OfferRegistrySubscriptionImpl extends BusResource implements
  OfferRegistrySubscription {
  final OfferRegistryObserverImpl observer;
  final tecgraf.openbus.core.v2_1.services.offer_registry
    .OfferRegistryObserver proxy;
  private final ArrayListMultimap<String, String> properties;
  private final OfferRegistryImpl registry;
  private OfferRegistryObserverSubscription sub = null;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected OfferRegistrySubscriptionImpl(OfferRegistryImpl registry,
    OfferRegistryObserverImpl observer, tecgraf.openbus.core.v2_1.services.
    offer_registry.OfferRegistryObserver proxy, ArrayListMultimap<String,
    String> properties) {
    this.registry = registry;
    this.observer = observer;
    this.proxy = proxy;
    this.properties = properties;
  }

  @Override
  public boolean subscribed() throws ServiceFailure {
    while (true) {
      try {
        return subscribed(Integer.MAX_VALUE);
      } catch (TimeoutException ignored) {
      }
    }
  }

  @Override
  public boolean subscribed(long timeoutMillis) throws ServiceFailure,
    TimeoutException {
    if (timeoutMillis < 0) {
      throw new IllegalArgumentException("O timeout deve ser positivo.");
    }
    synchronized (lock) {
      // O while é necessário para retestar sub após o wait, antes de
      // retornar, devido à possibilidade de sinais espúrios. A contagem de
      // tempo abaixo pode ser altamente inacurada devido a preempções, mas o
      // parâmetro se refere ao tempo total passado, então não há problema.
      // OBS: timeoutMillis não pode ser 0, senão wait aguardará
      // indefinidamente.
      while (sub == null && lastError == null && timeoutMillis >= 0) {
        long initial = System.currentTimeMillis();
        if (cancelled) {
          return false;
        }
        checkLoggedOut();
        try {
          if (timeoutMillis > 0) {
            lock.wait(timeoutMillis);
          }
        } catch (InterruptedException e) {
          logInterruptError(logger, e);
          Thread.currentThread().interrupt();
          return false;
        }
        timeoutMillis -= System.currentTimeMillis() - initial;
      }
      if (cancelled) {
        return false;
      }
      if (sub == null) {
        if (lastError == null) {
          throw new TimeoutException("Não foi possível registrar/obter a oferta" +
            " remota no tempo especificado.");
        } else {
          try {
            throw lastError;
          } catch (ServiceFailure | SystemException e) {
            throw e;
          } catch (Throwable e) {
            throw new OpenBusInternalException("Exceção inesperada ao " +
              "tentar realizar uma subscrição de registro de oferta. Por " +
              "favor contacte o administrador do sistema e informe-o sobre " +
              "este erro.", e);
          }
        }
      }
      return true;
    }
  }

  @Override
  public ArrayListMultimap<String, String> properties() {
    return properties;
  }

  @Override
  public void remove() {
    registry.removeRegistrySubscription(this);
    removeSub();
  }

  @Override
  public OfferRegistryObserver observer() {
    return observer.observer;
  }

  protected void sub(OfferRegistryObserverSubscription sub) {
    synchronized (lock) {
      this.sub = sub;
      this.lastError = null;
      this.loggedOut = false;
      lock.notifyAll();
    }
  }

  protected void removeSub() {
    synchronized (lock) {
      sub = null;
      lastError = null;
      cancelled = true;
      lock.notifyAll();
    }
  }
}
