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
 * Representa��o local de uma inscri��o de observa��o de registro de oferta
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
      // O while � necess�rio para retestar sub ap�s o wait, antes de
      // retornar, devido � possibilidade de sinais esp�rios. A contagem de
      // tempo abaixo pode ser altamente inacurada devido a preemp��es, mas o
      // par�metro se refere ao tempo total passado, ent�o n�o h� problema.
      // OBS: timeoutMillis n�o pode ser 0, sen�o wait aguardar�
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
          throw new TimeoutException("N�o foi poss�vel registrar/obter a oferta" +
            " remota no tempo especificado.");
        } else {
          try {
            throw lastError;
          } catch (ServiceFailure | SystemException e) {
            throw e;
          } catch (Throwable e) {
            throw new OpenBusInternalException("Exce��o inesperada ao " +
              "tentar realizar uma subscri��o de registro de oferta. Por " +
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
