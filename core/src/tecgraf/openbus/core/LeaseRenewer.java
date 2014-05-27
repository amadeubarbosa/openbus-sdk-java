/*
 * $Id: LeaseRenewer.java 121064 2011-08-16 12:04:52Z hroenick $
 */
package tecgraf.openbus.core;

import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.access_control.AccessControl;

/**
 * Respons�vel por renovar um lease junto a um provedor.
 * 
 * @author Tecgraf
 */
final class LeaseRenewer {
  /**
   * Inst�ncia de logging.
   */
  private static final Logger logger = Logger.getLogger(LeaseRenewer.class
    .getName());
  /**
   * O nome da <i>thread</i> onde a renova��o do lease � efetuada.
   */
  private static final String RENEWER_THREAD_NAME = "RenewerThread";
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
   * @param conn a conex�o.
   * @param defaultLease tempo padr�o de lease.
   */
  public LeaseRenewer(Connection conn, int defaultLease) {
    this.renewer = new RenewerTask(conn, defaultLease);
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
      if (this.renewer.isSleeping()) {
        this.renewerThread.interrupt();
      }
      this.renewerThread = null;
    }
  }

  /**
   * Tarefa respons�vel por renovar um <i>lease</i>.
   * 
   * @author Tecgraf
   */
  private static class RenewerTask implements Runnable {

    /**
     * Indica se a <i>thread</i> deve continuar executando.
     */
    private volatile boolean mustContinue;
    /**
     * Indica se a <i>thread</i> est� dormindo.
     */
    private volatile boolean isSleeping;

    /**
     * A conex�o.
     */
    private WeakReference<Connection> weakConn;
    /**
     * Tempo padr�o de lease.
     */
    private int defaultLease;

    /**
     * Cria uma tarefa para renovar um <i>lease</i>.
     * 
     * @param conn a conex�o.
     * @param defaultLease
     */
    RenewerTask(Connection conn, int defaultLease) {
      this.mustContinue = true;
      this.isSleeping = false;
      this.weakConn = new WeakReference<Connection>(conn);
      this.defaultLease = defaultLease;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      // TODO [OPENBUS-1849] Avaliar uso de join thread
      while (this.mustContinue) {
        Connection conn = weakConn.get();
        if (conn == null) {
          this.mustContinue = false;
          break;
        }
        OpenBusContextImpl context = null;
        int lease = -1;
        try {
          context =
            (OpenBusContextImpl) conn.orb().resolve_initial_references(
              "OpenBusContext");
          context.setCurrentConnection(conn);
          AccessControl access = ((ConnectionImpl) conn).access();
          if (access != null) {
            lease = access.renew();
            this.mustContinue &= (lease > 0);
          }
        }
        catch (InvalidName e) {
          String message = "Falha inesperada ao obter o contexto.";
          logger.log(Level.SEVERE, message, e);
          this.mustContinue = false;
        }
        catch (NO_PERMISSION ne) {
          this.mustContinue = false;
        }
        catch (Exception e) {
          logger.log(Level.SEVERE, "Falha na renova��o da credencial", e);
        }
        finally {
          if (context != null) {
            context.setCurrentConnection(null);
          }
        }

        if (this.mustContinue) {
          try {
            int time = lease;
            if (time < 0) {
              time = this.defaultLease;
            }
            this.isSleeping = true;
            logger.finest("Thread de renova��o indo dormir.");
            Thread.sleep(time * 1000);
            logger.finest("Thread de renova��o acordando.");
            this.isSleeping = false;
          }
          catch (InterruptedException e) {
            this.mustContinue = false;
            this.isSleeping = false;
            break;
          }
        }
      }
      logger.finest("Thread de renova��o terminou loop de renova��o");
    }

    /**
     * Finaliza o renovador de <i>lease</i>.
     */
    public void finish() {
      this.mustContinue = false;
    }

    /**
     * Verifica se a thread est� dormindo no loop de renova��o de credencial.
     * 
     * @return <code>true</code> se a thread estiver dormindo, e
     *         <code>false</code> caso contr�rio.
     */
    public boolean isSleeping() {
      return this.isSleeping;
    }
  }
}
