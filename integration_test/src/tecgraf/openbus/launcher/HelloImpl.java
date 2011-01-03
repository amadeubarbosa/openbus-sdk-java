package tecgraf.openbus.launcher;

import testidl.hello.IHelloOperations;

/**
 * Implementa as operações da interface IHello.
 * 
 * @author Tecgraf
 */
public final class HelloImpl implements IHelloOperations {

  /**
   * {@inheritDoc}
   */
  public boolean sayHello() {
    System.out.println("Hello World !!");
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public boolean sayHelloName(String name) {
    System.out.println(String.format("Hello %s !!", name));
    return true;
  }

}
