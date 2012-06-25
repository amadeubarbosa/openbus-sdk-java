package tecgraf.openbus.interop.simple;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

/**
 * Implementa��o do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloServant extends HelloPOA {
  /**
   * Conex�o com o barramento.
   */
  private Connection conn;

  /**
   * Construtor.
   * 
   * @param conn Conex�o com o barramento.
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
      LoginInfo caller = callerChain.caller();
      System.out.println(String.format("Hello from %s!", caller.entity));
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }
}
