/*
 * $Id$
 */
package tecgraf.openbus.lease;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.ILeaseProvider;

/**
 * Responsável por renovar um lease junto a um provedor.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class LeaseRenewer {
  /**
   * O nome da <i>thread</i> onde a renovação do lease é efetuada.
   */
  private static final String RENEWER_THREAD_NAME = "RenewerThread";
  /**
   * O tempo padrão para a renovação do lease.
   */
  private static final int DEFAULT_LEASE = 30;
  /**
   * A tarefa responsável por renovar um <i>lease</i>.
   */
  private RenewerTask renewer;
  /**
   * A thread de renovação.
   */
  private Thread renewerThread;

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
   * Inicia uma renovação de <i>lease</i>.
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
   * Solicita o fim da renovação do <i>lease</i>.
   */
  public synchronized void stop() {
    if (this.renewerThread != null) {
      this.renewer.finish();
      if (this.renewer.isSleeping()) {
        this.renewerThread.interrupt();
      }
      this.renewerThread = null;
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
     * Indica se a <i>thread</i> deve continuar executando.
     */
    private volatile boolean mustContinue;
    /**
     * Indica se a <i>thread</i> está dormindo.
     */
    private volatile boolean isSleeping;
    /**
     * O provedor do <i>lease</i>.
     */
    ILeaseProvider provider;
    /**
     * <i>Callback</i> usada para informar que a renovação de um <i>lease</i>
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
      this.isSleeping = false;
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
    public void run() {
      Logger logger = LoggerFactory.getLogger(LeaseRenewer.class);

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
            logger.warn("Falha na renovação da credencial.");
            this.mustContinue = false;
            if (this.expiredCallback != null) {
              logger.debug("Chamando a callback de expiração da credencial.");
              this.expiredCallback.expired();
            }
          }
          else {
            logger.debug(
              "Credencial renovada. Próxima renovação em {} segundos.",
              newLease.value);
            lease = newLease.value;
          }
        }
        catch (SystemException e) {
          logger.error("Falha na renovação da credencial", e);
        }

        if (this.mustContinue) {
          try {
            this.isSleeping = true;
            Thread.sleep(lease * 1000);
            this.isSleeping = false;
          }
          catch (InterruptedException e) {
            this.mustContinue = false;
            this.isSleeping = false;
            logger.debug("Thread interrompida!");
          }
        }
      }

      logger.info("Finalizando a renovação do lease.");
    }

    /**
     * Finaliza o renovador de <i>lease</i>.
     */
    public void finish() {
      this.mustContinue = false;
    }

    /**
     * Verifica se a thread está dormindo no loop de renovação de credencial.
     * 
     * @return <code>true</code> se a thread estiver dormindo, e
     *         <code>false</code> caso contrário.
     */
    public boolean isSleeping() {
      return this.isSleeping;
    }
  }
}
