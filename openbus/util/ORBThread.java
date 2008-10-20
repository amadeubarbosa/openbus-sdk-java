/*
 * $Id$
 */
package openbus.util;

import openbus.ORBWrapper;

/**
 * Responsável por executar e finalizar o ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class ORBThread extends Thread {
  /**
   * O ORB.
   */
  private ORBWrapper orb;

  /**
   * Cria a <i>thread</i> para execução do ORB.
   * 
   * @param orb O ORB.
   */
  public ORBThread(ORBWrapper orb) {
    this.orb = orb;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    this.orb.run();
  }

  /**
   * Finaliza o ORB.
   */
  public void finish() {
    this.orb.finish();
  }
}
