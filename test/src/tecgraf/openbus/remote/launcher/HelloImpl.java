package tecgraf.openbus.remote.launcher;

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
  @Override
  public void sayHello() {
    System.out.println("Hello World !!");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sayHelloName(String name) {
    System.out.println(String.format("Hello %s !!", name));
  }

}
