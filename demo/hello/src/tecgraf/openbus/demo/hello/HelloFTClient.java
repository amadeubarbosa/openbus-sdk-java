package tecgraf.openbus.demo.hello;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.UserException;

import scs.core.IComponent;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.fault_tolerance.v1_05.IFaultTolerantService;
import demoidl.hello.IHello;
import demoidl.hello.IHelloHelper;

public class HelloFTClient {
  public static void main(String[] args) throws OpenBusException,
    UserException, IOException {
    Properties props = new Properties();
    InputStream in =
      HelloFTClient.class.getResourceAsStream("/Hello.properties");
    try {
      props.load(in);
    }
    finally {
      in.close();
    }

    String host = props.getProperty("host.name");
    String portString = props.getProperty("host.port");
    int port = Integer.valueOf(portString);

    Properties orbProps = new Properties();
    orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
    Openbus bus = Openbus.getInstance();
    bus.initWithFaultTolerance(args, orbProps, host, port);

    String userLogin = props.getProperty("login");
    String userPassword = props.getProperty("password");

    IRegistryService registryService = bus.connect(userLogin, userPassword);
    if (registryService == null) {
      throw new RSUnavailableException();
    }

    ServiceOffer[] servicesOffers =
      registryService.find(new String[] { "IHello" });
    ServiceOffer serviceOffer = servicesOffers[0];
    IComponent component = serviceOffer.member;
    org.omg.CORBA.Object helloObject = component.getFacetByName("IHello");
    IHello hello = IHelloHelper.narrow(helloObject);

    IFaultTolerantService ft = bus.getACSFaultTolerantService();
    while (true) {
      hello.sayHello();
      try {
        ft.isAlive();
      }
      catch (Exception e) {
        System.out.println("ERRO!");
      }

    }
  }
}
