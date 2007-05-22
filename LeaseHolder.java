/*
 * Última alteração: $Id$
 */
package openbus.common;

import java.util.Timer;
import java.util.TimerTask;

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
   * Responsável por executar o método de renovação a cada intervalo.
   */
  private Timer timer;

  /**
   * Cria um renovador de <i>lease</i> junto a um provedor.
   * 
   * @param credential A credencial que deve ser renovada.
   * @param leaseProvider O provedor onde o <i>lease</i> deve ser renovado.
   */
  public LeaseHolder(Credential credential, ILeaseProvider leaseProvider) {
    this.credential = credential;
    this.leaseProvider = leaseProvider;
    this.timer = new Timer();
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
   * 
   * @param lease O tempo do lease (em segundos).
   * 
   * @return {@code true} caso o <i>lease</i> possa ser renovado, ou
   *         {@code false}, caso o tempo recebido seja inválido (menor ou igual
   *         a 0).
   */
  public boolean startRenew(int lease) {
    if (lease <= 0) {
      return false;
    }
    TimerTask task = new RenewerTask(this, this.credential, this.leaseProvider);
    this.timer.schedule(task, lease * 1000);
    return true;
  }

  /**
   * Solicita o fim da renovação do <i>lease</i>.
   */
  public void stopRenew() {
    this.timer.cancel();
  }

  /**
   * Tarefa responsável por renovar um <i>lease</i>.
   * 
   * @author Tecgraf/PUC-Rio
   */
  private static class RenewerTask extends TimerTask {
    /**
     * O responsável pela renovação do <i>lease</i>.
     */
    private LeaseHolder leaseHolder;
    /**
     * O provedor do <i>lease</i>.
     */
    private ILeaseProvider provider;
    /**
     * A credencial correspondente ao <i>lease</i>.
     */
    private Credential credential;

    /**
     * Cria uma tarefa para renovar um <i>lease</i>.
     * 
     * @param leaseHolder O responsável pela renovação do <i>lease</i>.
     * @param credential A credencial correspondente ao <i>lease</i>.
     * @param provider O provedor do <i>lease</i>.
     */
    RenewerTask(LeaseHolder leaseHolder, Credential credential,
      ILeaseProvider provider) {
      this.leaseHolder = leaseHolder;
      this.credential = credential;
      this.provider = provider;
    }

    @Override
    public void run() {
      IntHolder newLease = new IntHolder();
      this.provider.renewLease(this.credential, newLease);
      leaseHolder.startRenew(newLease.value);
    }
  }
}