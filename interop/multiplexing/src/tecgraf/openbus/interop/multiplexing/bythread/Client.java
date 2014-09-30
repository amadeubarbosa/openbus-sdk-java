package tecgraf.openbus.interop.multiplexing.bythread;

import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;

/**
 * Cliente do demo de Multiplexação.
 * 
 * @author Tecgraf
 */
public class Client {
  /**
   * Função principal.
   * 
   * @param args
   */
  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      String host2 = props.getProperty("bus2.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      int port2 = Integer.valueOf(props.getProperty("bus2.host.port"));
      BusAddress bus1 = new BusAddress(host, port);
      BusAddress bus2 = new BusAddress(host2, port2);
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      BusAddress buses[] = { bus1, bus2 };

      for (BusAddress busAddr : buses) {
        ORB orb = ORBInitializer.initORB();
        OpenBusContext context =
          (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
        Connection conn =
          context.connectByAddress(busAddr.hostname, busAddr.port);
        context.setDefaultConnection(conn);
        String login = "interop_multiplexing_java_client";
        conn.loginByPassword(login, login.getBytes());

        ServiceProperty[] serviceProperties = new ServiceProperty[2];
        serviceProperties[0] =
          new ServiceProperty("openbus.component.interface", HelloHelper.id());
        serviceProperties[1] =
          new ServiceProperty("offer.domain", "Interoperability Tests");
        ServiceOfferDesc[] services =
          context.getOfferRegistry().findServices(serviceProperties);
        for (ServiceOfferDesc offer : services) {
          for (ServiceProperty prop : offer.properties) {
            if (prop.name.equals("openbus.offer.entity")) {
              System.out.println("found offer from " + prop.value
                + " on bus at " + busAddr.hostname + ":" + busAddr.port);
            }
          }
          org.omg.CORBA.Object obj =
            offer.service_ref.getFacet(HelloHelper.id());
          Hello hello = HelloHelper.narrow(obj);
          String expected = String.format("Hello %s@%s!", login, conn.busid());
          String sayHello = hello.sayHello();
          if (expected.equals(sayHello)) {
            System.out.println("Received: " + sayHello);
          }
          else {
            System.err.println("ERROR!");
            System.err.println("Expected: " + expected);
            System.err.println("Received: " + sayHello);
          }
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class BusAddress {
    public String hostname;
    public int port;

    public BusAddress(String host, int port) {
      this.hostname = host;
      this.port = port;
    }
  }
}
