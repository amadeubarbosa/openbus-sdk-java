package tecgraf.openbus.launcher;

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
   * Contrutor.
   */
  public HelloServant() {
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
