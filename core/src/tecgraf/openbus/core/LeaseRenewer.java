/*
 * $Id: LeaseRenewer.java 121064 2011-08-16 12:04:52Z hroenick $
 */
package tecgraf.openbus.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.NO_PERMISSION;

import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

/**
 * Responsável por renovar um lease junto a um provedor.
 * 
 * @author Tecgraf
 */
public final class LeaseRenewer {
  private static final Logger logger = Logger.getLogger(ConnectionImpl.class
    .getName());
  /**
   * O nome da <i>thread</i> onde a renovação do lease é efetuada.
   */
  private static final String RENEWER_THREAD_NAME = "RenewerThread";
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
   */
  public LeaseRenewer(Connection conn) {
    this.renewer = new RenewerTask(conn);
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
   * @author Tecgraf
   */
  private static class RenewerTask implements Runnable {

    /**
     * Indica se a <i>thread</i> deve continuar executando.
     */
    private volatile boolean mustContinue;
    /**
     * Indica se a <i>thread</i> está dormindo.
     */
    private volatile boolean isSleeping;

    private AccessControl manager;
    private LoginInfo login;
    private Connection conn;

    /**
     * Cria uma tarefa para renovar um <i>lease</i>.
     */
    RenewerTask(Connection conn) {
      this.mustContinue = true;
      this.isSleeping = false;
      this.login = conn.login();
      this.manager = ((ConnectionImpl) conn).access();
      this.conn = conn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      while (this.mustContinue) {
        int lease = -1;
        try {
          boolean expired;
          try {
            lease = this.manager.renew();
            expired = !(lease > 0);
            LoginInfo info = this.conn.login();
            logger.info(String.format(
              "Renovando o login '%s' da entidade '%s' por %d segs.", info.id,
              info.entity, lease));
          }
          catch (NO_PERMISSION ne) {
            expired = true;
          }

          if (expired) {
            this.mustContinue = false;
          }
        }
        catch (ServiceFailure e) {
          logger.log(Level.SEVERE, "Falha na renovação da credencial", e);
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
          }
        }
        lease = -1;
      }
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
