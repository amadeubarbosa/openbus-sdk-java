/*
 * $Id$
 */
package tecgraf.openbus.lease;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import openbusidl.acs.Credential;
import openbusidl.acs.ILeaseProvider;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.SystemException;

import tecgraf.openbus.util.Log;

/**
 * Respons�vel por renovar um lease junto a um provedor.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class LeaseRenewer {
  /**
   * A tarefa respons�vel por renovar um <i>lease</i>.
   */
  private RenewerTask renewer;
  /**
   * A thread de renova��o.
   */
  private Thread renewerThread;
  /**
   * Indica se a thread est� em execu��o.
   */
  private boolean running;
  /**
   * Sincroniza os m�todos start() e finish().
   */
  private Lock lock;

  /**
   * Cria um renovador de <i>lease</i> junto a um provedor.
   * 
   * @param credential A credencial que deve ser renovada.
   * @param leaseProvider O provedor onde o <i>lease</i> deve ser renovado.
   * @param expiredCallback <i>Callback</i> usada para informar que a renova��o
   *        de um <i>lease</i> falhou.
   */
  public LeaseRenewer(Credential credential, ILeaseProvider leaseProvider,
    LeaseExpiredCallback expiredCallback) {
    this.renewer = new RenewerTask(credential, leaseProvider, expiredCallback);
    this.running = false;
    this.lock = new ReentrantLock();
  }

  /**
   * Define o observador do <i>lease</i>.
   * 
   * @param lec O observador do <i>lease</i>.
   */
  public void setLeaseExpiredCallback(LeaseExpiredCallback lec) {
    this.renewer.expiredCallback = lec;
  }

  /**
   * Inicia uma renova��o de <i>lease</i>.
   */
  public void start() {
    lock.lock();
    try {
      if (!running) {
        this.running = true;
        this.renewerThread = new Thread(this.renewer);
        this.renewerThread.setDaemon(true);
        this.renewerThread.start();
      }
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Solicita o fim da renova��o do <i>lease</i>.
   */
  public void stop() {
    lock.lock();
    try {
      if (running) {
        this.renewerThread.interrupt();
        this.renewerThread = null;
        this.running = false;
      }
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Tarefa respons�vel por renovar um <i>lease</i>.
   * 
   * @author Tecgraf/PUC-Rio
   */
  private static class RenewerTask implements Runnable {
    /**
     * A credencial correspondente ao <i>lease</i>.
     */
    private Credential credential;
    /**
     * O provedor do <i>lease</i>.
     */
    private ILeaseProvider provider;
    /**
     * <i>Callback</i> usada para informar que a renova��o de um <i>lease</i>
     * falhou.
     */
    private LeaseExpiredCallback expiredCallback;

    /**
     * Cria uma tarefa para renovar um <i>lease</i>.
     * 
     * @param credential A credencial correspondente ao <i>lease</i>.
     * @param provider O provedor do <i>lease</i>.
     */
    RenewerTask(Credential credential, ILeaseProvider provider) {
      this.credential = credential;
      this.provider = provider;
    }

    /**
     * Cria uma tarefa para renovar um <i>lease</i>.
     * 
     * @param credential A credencial correspondente ao <i>lease</i>.
     * @param provider O provedor do <i>lease</i>.
     * @param expiredCallback <i>Callback</i> usada para informar que a
     *        renova��o de um <i>lease</i> falhou.
     */
    RenewerTask(Credential credential, ILeaseProvider provider,
      LeaseExpiredCallback expiredCallback) {
      this(credential, provider);
      this.expiredCallback = expiredCallback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      try {
        while (true) {
          IntHolder newLease = new IntHolder();
          try {
            if (!this.provider.renewLease(this.credential, newLease)) {
              Log.LEASE.warning("Falha na renova��o da credencial.");
              if (this.expiredCallback != null) {
                this.expiredCallback.expired();
              }
              return;
            }
          }
          catch (SystemException e) {
            Log.LEASE.severe(e.getMessage(), e);
          }
          StringBuilder msg = new StringBuilder();
          msg.append(new Date());
          msg.append(" - Lease renovado. Pr�xima renova��o em ");
          msg.append(newLease.value);
          msg.append(" segundos.");
          Log.LEASE.fine(msg.toString());
          Thread.sleep(newLease.value * 1000);
        }
      }
      catch (InterruptedException e) {
        // Quando for interrompida, a thread deve morrer, portanto,
        // precisa sair do while.
      }
    }

    /**
     * Define o provedor do <i>lease</i>.
     * 
     * @param provider O provedor do <i>lease</i>.
     */
    public void setProvider(ILeaseProvider provider) {
      this.provider = provider;
    }
  }
}