package tecgraf.openbus.core;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;

import java.util.logging.Level;
import java.util.logging.Logger;

abstract class BusResource {
  protected Throwable lastError = null;
  protected boolean loggedOut = false;
  protected boolean cancelled = false;
  // Não vale a pena usar ReentrantLock pois só tenho uma condição; E não
  // posso usar Monitor por causa do teste de loggedOut.
  protected final Object lock = new Object();

  public abstract void remove();

  protected void error(Throwable error) {
    synchronized (lock) {
      this.lastError = error;
      lock.notifyAll();
    }
  }

  protected void loggedOut() {
    synchronized (lock) {
      loggedOut = true;
      setCancelled();
    }
  }

  protected void setCancelled() {
    synchronized (lock) {
      cancelled = true;
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
    logger.log(Level.SEVERE, "Interrupção recebida ao aguardar por uma oferta" +
      " remota.", e);
  }
}
