package tecgraf.openbus.interop.multiplexing.bythread;

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
   * Contexto do barramento.
   */
  private OpenBusContext context;

  /**
   * Construtor.
   * 
   * @param conns Lista de conexões com o barramento.
   */
  public HelloServant(OpenBusContext conns) {
    this.context = conns;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String sayHello() {
    try {
      CallerChain callerChain = context.getCallerChain();
      Connection conn = context.getCurrentConnection();
      if (callerChain != null) {
        System.out.println(String.format("Calling in %s @ %s",
          conn.login().entity, conn.busid()));
        LoginInfo caller = callerChain.caller();
        String hello =
          String.format("Hello %s@%s!", caller.entity, callerChain.busid());
        System.out.println(hello);
        return hello;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "A bug happened! Bye!";
  }
}
