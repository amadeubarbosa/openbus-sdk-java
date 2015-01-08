package tecgraf.openbus.interop.reloggedjoin;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.interop.simple.HelloPOA;

/**
 * Implementação do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloServerServant extends HelloPOA {
  /**
   * Contexto do OpenBus em uso.
   */
  private OpenBusContext context;

  /**
   * Construtor.
   * 
   * @param context Conexão com o barramento.
   */
  public HelloServerServant(OpenBusContext context) {
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String sayHello() {
    try {
      CallerChain callerChain = context.getCallerChain();
      String entity = callerChain.caller().entity;
      LoginInfo[] originators = callerChain.originators();
      if (originators.length > 0) {
        entity = originators[0].entity;
      }
      return String.format("Hello %s!", entity);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "A bug happened! Bye!";
  }
}
