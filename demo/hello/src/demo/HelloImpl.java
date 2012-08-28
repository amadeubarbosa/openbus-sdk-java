package demo;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Implementa��o do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloImpl extends HelloPOA {
  /**
   * Contexto com o barramento.
   */
  private OpenBusContext context;

  /**
   * Construtor.
   * 
   * @param context Conex�o com o barramento.
   */
  public HelloImpl(OpenBusContext context) {
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sayHello() {
    try {
      CallerChain callerChain = context.getCallerChain();
      LoginInfo caller = callerChain.caller();
      String hello = String.format("Hello %s!", caller.entity);
      System.out.println(hello);
    }
    catch (Exception e) {
      System.out
        .println("Erro no m�todo sayHello ao obter a cadeia de chamadas:");
      e.printStackTrace(System.out);
    }
  }
}
