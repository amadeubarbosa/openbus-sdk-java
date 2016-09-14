package tecgraf.openbus.core;

import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.LocalOffer;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.exception.OpenBusInternalException;

import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

class LocalOfferImpl extends BusResource implements LocalOffer {
  public final IComponent service;
  public final ServiceProperty[] props;
  private final OfferRegistryImpl registry;
  private RemoteOfferImpl remote = null;
  private static final Logger logger = Logger.getLogger(LocalOfferImpl.class
    .getName());

  protected LocalOfferImpl(OfferRegistryImpl registry, IComponent service,
                           ServiceProperty[] props) {
    this.registry = registry;
    this.service = service;
    this.props = props;
  }

  @Override
  public Connection connection() {
    return registry.connection();
  }

  @Override
  public RemoteOffer remoteOffer() throws ServiceFailure, InvalidService,
    InvalidProperties, UnauthorizedFacets {
    while (true) {
      try {
        return remoteOffer(Integer.MAX_VALUE);
      } catch (TimeoutException ignored) {
      }
    }
  }

  @Override
  public RemoteOffer remoteOffer(long timeoutMillis) throws ServiceFailure,
    InvalidService, InvalidProperties, UnauthorizedFacets, TimeoutException {
    if (timeoutMillis < 0) {
      throw new IllegalArgumentException("O timeout deve ser positivo.");
    }
    synchronized (lock) {
      // O while é necessário para retestar remote após o wait, antes de
      // retornar, devido à possibilidade de sinais espúrios. A contagem de
      // tempo abaixo pode ser altamente inacurada devido a preempções, mas o
      // parâmetro se refere ao tempo total passado, então não há problema.
      // OBS: timeoutMillis não pode ser 0, senão wait aguardará
      // indefinidamente.
      while (remote == null && lastError == null && timeoutMillis >= 0) {
        long initial = System.currentTimeMillis();
        checkLoggedOut();
        if (cancelled) {
          return null;
        }
        try {
          if (timeoutMillis > 0) {
            lock.wait(timeoutMillis);
          }
        } catch (InterruptedException e) {
          logInterruptError(logger, e);
          Thread.currentThread().interrupt();
          return null;
        }
        timeoutMillis -= System.currentTimeMillis() - initial;
      }
      checkLoggedOut();
      if (cancelled) {
        return null;
      }
      if (remote == null) {
        if (lastError == null) {
          throw new TimeoutException("Não foi possível obter a oferta remota " +
            "no tempo especificado.");
        } else {
          try {
            throw lastError;
          } catch (ServiceFailure | InvalidService | InvalidProperties |
            UnauthorizedFacets | RuntimeException | Error e) {
            throw e;
          } catch (Throwable e) {
            throw new OpenBusInternalException("Exceção inesperada ao " +
              "tentar registrar uma oferta. Por favor contacte o " +
              "administrador do sistema e informe-o sobre este erro.", e);
          }
        }
      }
      return remote;
    }
  }

  @Override
  public void remove() {
    setCancelled();
    // o pedido de cancelamento tem que ser feito fora do bloco synchronized
    // para evitar deadlocks. Assim se a tarefa estiver a ponto de fazer um
    // set remote, ela poderá fazê-lo pois conseguirá adquirir o lock.
    registry.cancelRegisterTask(this);
    // a chamada abaixo apenas remove as referências locais. A remoção do
    // barramento é feita pelo método previamente chamado cancelRegisterTask do
    // registry.
    removeOffer();
  }

  protected void error(Throwable error) {
    synchronized (lock) {
      this.remote = null;
      super.error(error);
    }
  }

  protected void loggedOut() {
    synchronized (lock) {
      super.loggedOut();
      removeOffer();
    }
  }

  protected void remote(RemoteOfferImpl remote) {
    synchronized (lock) {
      this.remote = remote;
      this.lastError = null;
      lock.notifyAll();
    }
  }

  protected void removeOffer() {
    synchronized (lock) {
      if (remote != null) {
        remote.removed();
        remote = null;
      }
      lastError = null;
      lock.notifyAll();
    }
  }
}
