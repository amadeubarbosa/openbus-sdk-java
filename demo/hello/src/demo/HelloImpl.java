package demo;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Implementa��o do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloImpl extends HelloPOA {
  /**
   * Conex�o com o barramento.
   */
  private Connection conn;

  /**
   * Construtor.
   * 
   * @param conn Conex�o com o barramento.
   */
  public HelloImpl(Connection conn) {
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
