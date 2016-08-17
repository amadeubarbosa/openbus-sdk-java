package tecgraf.openbus.core;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scs.core.IComponent;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.*;
import tecgraf.openbus.RemoteOffer;

class LocalOfferImpl implements LocalOffer {
  public final IComponent service;
  public final ServiceProperty[] props;
  private final OfferRegistryImpl registry;
  private RemoteOfferImpl remote;
  private boolean loggedOut;
  // Não vale a pena usar ReentrantLock pois só tenho uma condição; E não
  // posso usar Monitor por causa do teste de loggedOut.
  private final Object lock = new Object();
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected LocalOfferImpl(OfferRegistryImpl registry, IComponent service,
                           ServiceProperty[] props) {
    this.registry = registry;
    this.service = service;
    this.props = props;
  }

  @Override
  public RemoteOffer remoteOffer() {
    synchronized (lock) {
      // O while é necessário para retestar remote após o wait, antes de
      // retornar, devido à possibilidade de sinais espúrios.
      while (remote == null) {
        checkLoggedOut();
        try {
          lock.wait();
        } catch (InterruptedException e) {
          logInterruptError(e);
          Thread.currentThread().interrupt();
          return null;
        }
      }
      return remote;
    }
  }

  @Override
  public RemoteOffer remoteOffer(long timeoutmillis, int nanos) {
    if (timeoutmillis < 0) {
      throw new IllegalArgumentException("O timeout deve ser positivo.");
    }
    synchronized (lock) {
      // O while é necessário para retestar remote após o wait, antes de
      // retornar, devido à possibilidade de sinais espúrios. A contagem de
      // tempo abaixo pode ser altamente inacurada devido a preempções, mas o
      // parâmetro se refere ao tempo total passado, então não há problema.
      while (remote == null && timeoutmillis > 0) {
        long initial = System.currentTimeMillis();
        checkLoggedOut();
        try {
          lock.wait(timeoutmillis, nanos);
        } catch (InterruptedException e) {
          logInterruptError(e);
          Thread.currentThread().interrupt();
          return null;
        }
        timeoutmillis -= System.currentTimeMillis() - initial;
      }
      return remote;
    }
  }

  @Override
  public void remove() {
    // o pedido de cancelamento tem que ser feito fora do bloco synchronized
    // para evitar deadlocks. Assim se a tarefa estiver a ponto de fazer um
    // set remote, ela poderá fazê-lo pois conseguirá adquirir o lock.
    registry.cancelRegisterTask(this);
    // a chamada abaixo apenas remove as referências locais. A remoção do
    // barramento é feita pelo método previamente chamado cancelRegisterTask do
    // registry.
    removeOffer();
  }

  protected void remote(RemoteOfferImpl remote) {
    synchronized (lock) {
      this.remote = remote;
      this.loggedOut = false;
      lock.notifyAll();
    }
  }

  protected void loggedOut() {
    synchronized (lock) {
      this.remote = null;
      this.loggedOut = true;
      lock.notifyAll();
    }
  }

  protected void removeOffer() {
    synchronized (lock) {
      if (remote != null) {
        remote.removed();
        remote = null;
      }
    }
  }

  private void checkLoggedOut() {
    synchronized (lock) {
      if (loggedOut) {
        throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus
          .COMPLETED_NO);
      }
    }
  }

  private void logInterruptError(Exception e) {
    logger.error("Interrupção recebida ao aguardar por uma oferta remota.", e);
  }
}
