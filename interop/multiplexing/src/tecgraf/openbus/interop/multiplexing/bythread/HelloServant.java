package tecgraf.openbus.interop.multiplexing.bythread;

import java.util.List;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
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
  private List<Connection> conns;

  /**
   * Construtor.
   * 
   * @param conns Lista de conexões com o barramento.
   */
  public HelloServant(List<Connection> conns) {
    this.conns = conns;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String sayHello() {
    try {
      for (Connection conn : conns) {
        CallerChain callerChain = conn.getCallerChain();
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
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "A bug happened! Bye!";
  }
}
