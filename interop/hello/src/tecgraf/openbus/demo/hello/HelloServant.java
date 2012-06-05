package tecgraf.openbus.intereop.simple;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

/**
 * Implementação do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloServant extends HelloPOA {
  /**
   * Conexão com o barramento.
   */
  private Connection conn;

  /**
   * Construtor.
   * 
   * @param conn Conexão com o barramento.
   */
  public HelloServant(Connection conn) {
    this.conn = conn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sayHello() {
    try {
      CallerChain callerChain = conn.getCallerChain();
      LoginInfo[] callers = callerChain.callers();
      String entity = callers[callers.length - 1].entity;
      System.out.println(String.format("Hello from %s!", entity));
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }
}
