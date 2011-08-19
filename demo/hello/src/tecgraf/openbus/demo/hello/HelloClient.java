package tecgraf.openbus.demo.hello;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.UserException;

import scs.core.IComponent;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.Property;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.util.Log;
import demoidl.hello.IHello;
import demoidl.hello.IHelloHelper;

public class HelloClient {
  public static void main(String[] args) throws OpenBusException,
    UserException, IOException {
    
    boolean instrumentationActive = true;
  //Arguments checking
    for (String arg:args) {
      if (arg.equalsIgnoreCase("-di")) instrumentationActive = false; 
    }
    
    Properties props = new Properties();
    InputStream in = HelloClient.class.getResourceAsStream("/Hello.properties");
    try {
      props.load(in);
    }
    finally {
      in.close();
    }

    String host = props.getProperty("host.name");
    String portString = props.getProperty("host.port");
    int port = Integer.valueOf(portString);

    Log.setLogsLevel(Level.WARNING);
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

    IComponent component=null;
    ServiceOffer[] servicesOffers =
      registryService.find(new String[] { "IHello" });
    if (servicesOffers.length == 0) {
      System.out.println("No service offer found for interface IHello.");
      System.exit(1);
    }
    
    for (ServiceOffer offer:servicesOffers) {
      for (Property prop:offer.properties) {
        System.out.println(prop.name);
        if (prop.name.equalsIgnoreCase("instrumentation")) {
          if (instrumentationActive) {
            if (prop.value[0].equalsIgnoreCase("active")) {
              System.out.println("Using IHello: Instrumentation ACTIVE.");
              component = offer.member;
              break;
            }  
          }
        }
        else {
          if (!instrumentationActive) {
            component = offer.member;
            break;
          }
        }
        if (component != null) break;
      }
    }
    if (component == null) {
      System.out.println("Probably no service found with instrumentation active.");
      System.exit(1);
    }
    org.omg.CORBA.Object helloObject = component.getFacetByName("IHello");
    IHello hello = IHelloHelper.narrow(helloObject);
    FileWriter w=new FileWriter(new File("times-no-instrumentation.txt"));
    ArrayList<Long> times = new ArrayList<Long>();
    
    for (int i=0; i<20;i++){
      long initialTime = System.nanoTime();
      hello.sayHello();
      long finalTime = System.nanoTime() - initialTime;
      System.out.printf("Registered time: %d",finalTime);
      w.write(String.format("%d \n",finalTime));
      times.add(new Long(finalTime));
    }
    bus.disconnect();
    w.close();
    System.out.println("FIM");
    for (Long l:times) System.out.printf("%d \n",l.longValue());
  }
}
