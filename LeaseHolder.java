/*
 * $Id$
 */
package openbus.common;

import openbusidl.acs.Credential;
import openbusidl.acs.ILeaseProvider;

import org.omg.CORBA.IntHolder;

/**
 * Responsável por renovar um lease junto a um provedor.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class LeaseHolder {
  /**
   * A credencial que deve ser renovada.
   */
  private Credential credential;
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
   */
  public LeaseHolder(Credential credential, ILeaseProvider leaseProvider) {
    this.credential = credential;
    this.leaseProvider = leaseProvider;
    this.renewer = new RenewerTask(this.credential, this.leaseProvider);
  }

  /**
   * Define o provedor onde o <i>lease</i> deve ser renovado.
   * 
   * @param leaseProvider O provedor onde o <i>lease</i> deve ser renovado.
   */
  public void setProvider(ILeaseProvider leaseProvider) {
    this.leaseProvider = leaseProvider;
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
     * O provedor do <i>lease</i>.
     */
    private ILeaseProvider provider;
    /**
     * A credencial correspondente ao <i>lease</i>.
     */
    private Credential credential;
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

    @Override
    public void run() {
      while (this.mustContinue) {
        IntHolder newLease = new IntHolder();
        if (!this.provider.renewLease(this.credential, newLease)) {
          return;
        }
        try {
          Thread.sleep(newLease.value * 1000);
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    /**
     * Finaliza o renovador de <i>lease</i>.
     */
    public void finish() {
      this.mustContinue = false;
    }
  }
}