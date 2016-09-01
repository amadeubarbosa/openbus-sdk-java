package tecgraf.openbus.core;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import org.slf4j.Logger;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;

abstract class BusResource {
  protected Exception lastError = null;
  protected boolean loggedOut;
  protected boolean cancelled = false;
  // N�o vale a pena usar ReentrantLock pois s� tenho uma condi��o; E n�o
  // posso usar Monitor por causa do teste de loggedOut.
  protected final Object lock = new Object();

  public abstract void remove();

  protected void error(Exception error) {
    synchronized (lock) {
      this.lastError = error;
      this.loggedOut = false;
      lock.notifyAll();
    }
  }

  protected void loggedOut() {
    synchronized (lock) {
      loggedOut = true;
    }
  }

  protected void checkLoggedOut() {
    synchronized (lock) {
      if (loggedOut) {
        throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus
          .COMPLETED_NO);
      }
    }
  }

  protected void logInterruptError(Logger logger, Exception e) {
    logger.error("Interrup��o recebida ao aguardar por uma oferta remota.", e);
  }
}
