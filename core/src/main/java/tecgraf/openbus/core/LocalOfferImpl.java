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
  // N�o vale a pena usar ReentrantLock pois s� tenho uma condi��o; E n�o
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
      // O while � necess�rio para retestar remote ap�s o wait, antes de
      // retornar, devido � possibilidade de sinais esp�rios.
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
      // O while � necess�rio para retestar remote ap�s o wait, antes de
      // retornar, devido � possibilidade de sinais esp�rios. A contagem de
      // tempo abaixo pode ser altamente inacurada devido a preemp��es, mas o
      // par�metro se refere ao tempo total passado, ent�o n�o h� problema.
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
    // set remote, ela poder� faz�-lo pois conseguir� adquirir o lock.
    registry.cancelRegisterTask(this);
    // a chamada abaixo apenas remove as refer�ncias locais. A remo��o do
    // barramento � feita pelo m�todo previamente chamado cancelRegisterTask do
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
    logger.error("Interrup��o recebida ao aguardar por uma oferta remota.", e);
  }
}
