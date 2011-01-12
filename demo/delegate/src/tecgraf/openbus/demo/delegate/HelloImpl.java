package tecgraf.openbus.demo.delegate;

import org.omg.CORBA.Object;

import scs.core.servant.ComponentContext;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_06.access_control_service.Credential;
import demoidl.demoDelegate.IHelloPOA;

public final class HelloImpl extends IHelloPOA {
  private ComponentContext context;

  public HelloImpl(ComponentContext context) {
    this.context = context;
  }

  @Override
  public Object _get_component() {
    return this.context.getIComponent();
  }

  public void sayHello(String name) {
    Openbus openbus = Openbus.getInstance();
    Credential credencial = openbus.getInterceptedCredential();
    String message =
      String.format("[Thread %s]: Hello %s !", credencial.delegate, name);
    System.out.println(message);
  }

}
