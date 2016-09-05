package tecgraf.openbus.interop.simple;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Implementação do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloServant extends HelloPOA {
  /**
   * Contexto do OpenBus em uso.
   */
  private OpenBusContext context;

  /**
   * Construtor.
   * 
   * @param context Conexão com o barramento.
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
      CallerChain callerChain = context.callerChain();
      LoginInfo caller = callerChain.caller();
      return String.format("Hello %s!", caller.entity);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "A bug happened! Bye!";
  }
}
