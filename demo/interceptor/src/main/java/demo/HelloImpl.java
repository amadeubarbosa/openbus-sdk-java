package demo;

import org.omg.CORBA.ORB;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import demo.interceptor.ContextInspector;

/**
 * Implementação do componente Hello
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
   * @param context Conexão com o barramento.
   */
  public HelloImpl(OpenBusContext context) {
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sayHello() {
    ORB orb = context.orb();
    // recupera informação incluída no contexto
    ContextInspector inspector = ContextInspector.getContextInspector(orb);
    String data = inspector.getContextInformation();
    System.out.println(String.format(
      "Recebi a seguinte informação pelo contexto: '%s'", data));
    // trata a requisição
    CallerChain callerChain = context.getCallerChain();
    LoginInfo caller = callerChain.caller();
    String hello = String.format("Hello %s!", caller.entity);
    System.out.println(hello);
  }
}
