/*
 * $Id$
 */
package tecgraf.openbus.lease;

import java.util.Date;

import openbusidl.acs.Credential;
import openbusidl.acs.ILeaseProvider;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;

import tecgraf.openbus.util.Log;

/**
 * Respons�vel por renovar um lease junto a um provedor.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class LeaseRenewer {
  /**
   * O nome da <i>thread</i> onde a renova��o do lease � efetuada.
   */
  private static final String RENEWER_THREAD_NAME = "RenewerThread";
  /**
   * O tempo padr�o para a renova��o do lease.
   */
  private static final int DEFAULT_LEASE = 30;
  /**
   * A tarefa respons�vel por renovar um <i>lease</i>.
   */
  private RenewerTask renewer;
  /**
   * A thread de renova��o.
   */
  private Thread renewerThread;

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
  }

  /**
   * Define o provedor onde o <i>lease</i> deve ser renovado.
   * 
   * @param leaseProvider O provedor onde o <i>lease</i> deve ser renovado.
   */
  public void setProvider(ILeaseProvider leaseProvider) {
    this.renewer.provider = leaseProvider;
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
  public synchronized void start() {
    if (this.renewerThread != null) {
      this.stop();
    }
    this.renewerThread = new Thread(this.renewer, RENEWER_THREAD_NAME);
    this.renewerThread.setDaemon(true);
    this.renewerThread.start();
  }

  /**
   * Solicita o fim da renova��o do <i>lease</i>.
   */
  public synchronized void stop() {
    if (this.renewerThread != null) {
      this.renewer.finish();
      this.renewerThread.interrupt();
      this.renewerThread = null;
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
    * Indica se a <i>thread</i> deve continuar executando.
    */
    private boolean mustContinue;
    /**
     * O provedor do <i>lease</i>.
     */
    ILeaseProvider provider;
    /**
     * <i>Callback</i> usada para informar que a renova��o de um <i>lease</i>
     * falhou.
     */
    LeaseExpiredCallback expiredCallback;

    /**
     * Cria uma tarefa para renovar um <i>lease</i>.
     * 
     * @param credential A credencial correspondente ao <i>lease</i>.
     * @param provider O provedor do <i>lease</i>.
     */
    RenewerTask(Credential credential, ILeaseProvider provider) {
      this.credential = credential;
      this.provider = provider;
      this.mustContinue = true;
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
      int lease = DEFAULT_LEASE;

      while (this.mustContinue) {
        IntHolder newLease = new IntHolder();

        try {
          boolean expired;
          try {
            expired = !(this.provider.renewLease(this.credential, newLease));
          }
          catch (NO_PERMISSION ne) {
            expired = true;
          }

          if (expired) {
            Log.LEASE.warning("Falha na renova��o da credencial.");
            this.mustContinue = false;
            if (this.expiredCallback != null) {
              Log.LEASE.info("Chamando a callback de expira��o do lease.");
              this.expiredCallback.expired();
            }
          }
          else {
            StringBuilder msg = new StringBuilder();
            msg.append("Lease renovado. Pr�xima renova��o em ");
            msg.append(newLease.value);
            msg.append(" segundos.");
            Log.LEASE.fine(msg.toString());
            lease = newLease.value;
          }
        }
        catch (SystemException e) {
          Log.LEASE.severe(e.getMessage(), e);
        }

        if (this.mustContinue) {
          try {
            Thread.sleep(lease * 1000);
          }
          catch (InterruptedException e) {
            // Nada a ser feito.
          }
        }
      }

      Log.LEASE.info("Finalizando a renova��o do lease.");
    }

    /**
     * Finaliza o renovador de <i>lease</i>.
     */
    public void finish() {
      this.mustContinue = false;
    }
  }
}