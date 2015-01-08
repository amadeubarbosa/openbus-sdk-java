package tecgraf.openbus.interop.multiplexing;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.interop.simple.HelloPOA;

/**
 * Implementação do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloServant extends HelloPOA {
  /**
   * Contexto com o barramento.
   */
  private OpenBusContext context;

  /**
   * Construtor.
   * 
   * @param context Lista de conexões com o barramento.
   */
  public HelloServant(OpenBusContext context) {
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String sayHello() {
    try {
      CallerChain callerChain = context.getCallerChain();
      if (callerChain != null) {
        LoginInfo caller = callerChain.caller();
        String hello =
          String.format("Hello %s@%s!", caller.entity, callerChain.busid());
        return hello;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "A bug happened! Bye!";
  }
}
