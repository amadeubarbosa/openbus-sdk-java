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
 * Responsável por renovar um lease junto a um provedor.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class LeaseRenewer {
  /**
   * A tarefa responsável por renovar um <i>lease</i>.
   */
  private RenewerTask renewer;
  /**
   * A thread de renovação.
   */
  private Thread renewerThread;
  /**
   * Indica se a thread está em execução.
   */
  private boolean running;
  /**
   * Sincroniza os métodos start() e finish().
   */
  private Lock lock;

  /**
   * Cria um renovador de <i>lease</i> junto a um provedor.
   * 
   * @param credential A credencial que deve ser renovada.
   * @param leaseProvider O provedor onde o <i>lease</i> deve ser renovado.
   * @param expiredCallback <i>Callback</i> usada para informar que a renovação
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
   * Inicia uma renovação de <i>lease</i>.
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
   * Solicita o fim da renovação do <i>lease</i>.
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
   * Tarefa responsável por renovar um <i>lease</i>.
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
     * <i>Callback</i> usada para informar que a renovação de um <i>lease</i>
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
     *        renovação de um <i>lease</i> falhou.
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
              Log.LEASE.warning("Falha na renovação da credencial.");
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
          msg.append(" - Lease renovado. Próxima renovação em ");
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