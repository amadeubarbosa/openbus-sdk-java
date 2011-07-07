package tecgraf.openbus.demo.hello;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import demoidl.hello.IHelloPOA;

public final class HelloImpl extends IHelloPOA {
  public void sayHello() {
    Openbus openbus = Openbus.getInstance();
    Credential credential = openbus.getInterceptedCredential();
    String message;
    if (credential.delegate.equals("")) {
      message = String.format("Hello %s !!!", credential.owner);
    }
    else {
      message =
        String.format("Hello %s (%s) !!!", credential.delegate,
          credential.owner);
    }
    System.out.println(message);
  }
}
