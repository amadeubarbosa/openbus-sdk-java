package tecgraf.openbus.interop.simple;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

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
  public String sayHello() {
    try {
      CallerChain callerChain = conn.getCallerChain();
      LoginInfo caller = callerChain.caller();
      String hello = String.format("Hello %s!", caller.entity);
      System.out.println(hello);
      return hello;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "A bug happened! Bye!";
  }
}
