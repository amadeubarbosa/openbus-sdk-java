package tecgraf.openbus.demo.hello;

import scs.core.servant.ComponentContext;
import demoidl.hello.IHelloPOA;

public final class HelloImpl extends IHelloPOA {
  private ComponentContext context;

  public HelloImpl(ComponentContext context) {
    this.context = context;
  }

  public void sayHello() {
    System.out.println("Hello !!!");
  }
}
