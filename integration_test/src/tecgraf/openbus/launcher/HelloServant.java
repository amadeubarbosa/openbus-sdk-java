package tecgraf.openbus.launcher;

import scs.core.servant.ComponentContext;
import testidl.hello.IHelloOperations;
import testidl.hello.IHelloPOA;

/**
 * Servant da interface IHello.
 * 
 * @author Tecgraf
 */
public class HelloServant extends IHelloPOA {

  /**
   * Implementação do IHello.
   */
  private IHelloOperations operations;

  /**
   * Referencia para o ComponentContext do componente.
   */
  private ComponentContext context;

  /**
   * Contrutor obrigatório.
   * 
   * @param context
   */
  public HelloServant(ComponentContext context) {
    this.context = context;
    this.operations = new HelloImpl();
  }

  /**
   * {@inheritDoc}
   */
  public boolean sayHello() {
    return operations.sayHello();
  }

  /**
   * {@inheritDoc}
   */
  public boolean sayHelloName(String name) {
    return operations.sayHelloName(name);
  }

}
