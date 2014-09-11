package tecgraf.openbus.interop.multiplexing.byorb;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.interop.simple.HelloPOA;

/**
 * Implementação do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloServant extends HelloPOA {
  /**
   * Conexão com o barramento.
   */
  private OpenBusContext context;

  /**
   * Construtor.
   * 
   * @param conn Conexão com o barramento.
   */
  public HelloServant(OpenBusContext conn) {
    this.context = conn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String sayHello() {
    try {
      CallerChain callerChain = context.getCallerChain();
      LoginInfo caller = callerChain.caller();
      String hello = String.format("Hello %s!", caller.entity);
      System.out.println(hello);
      Connection conn = context.getCurrentConnection();
      System.out.println(String.format("From: %s : %s", conn.login().entity,
        conn.login().id));
      return hello;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "A bug happened! Bye!";
  }
}
