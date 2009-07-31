/*
 * $Id$
 */
package tecgraf.openbus.lease;

import java.util.Date;

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
   * O provedor onde o <i>lease</i> deve ser renovado.
   */
  private ILeaseProvider leaseProvider;
  /**
   * A tarefa responsável por renovar um <i>lease</i>.
   */
  private RenewerTask renewer;

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
    this.leaseProvider = leaseProvider;
    this.renewer = new RenewerTask(credential, this.leaseProvider,
      expiredCallback);
  }

  /**
   * Define o provedor onde o <i>lease</i> deve ser renovado.
   * 
   * @param leaseProvider O provedor onde o <i>lease</i> deve ser renovado.
   */
  public void setProvider(ILeaseProvider leaseProvider) {
    this.leaseProvider = leaseProvider;
    this.renewer.setProvider(this.leaseProvider);
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
    this.renewer.start();
  }

  /**
   * Solicita o fim da renovação do <i>lease</i>.
   */
  public void finish() {
    this.renewer.finish();
  }

  /**
   * Tarefa responsável por renovar um <i>lease</i>.
   * 
   * @author Tecgraf/PUC-Rio
   */
  private static class RenewerTask extends Thread {
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
     * Indica se a <i>thread</i> deve continuar executando.
     */
    private boolean mustContinue;

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
      while (this.mustContinue) {
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
        try {
          Thread.sleep(newLease.value * 1000);
        }
        catch (InterruptedException e) {
          // Nada a ser feito.
        }
      }
    }

    /**
     * Finaliza o renovador de <i>lease</i>.
     */
    public void finish() {
      this.mustContinue = false;
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