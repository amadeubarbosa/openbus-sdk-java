package tecgraf.openbus.remote.launcher;

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
  @Override
  public void sayHello() {
    operations.sayHello();

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sayHelloName(String name) {
    operations.sayHelloName(name);
  }

}
