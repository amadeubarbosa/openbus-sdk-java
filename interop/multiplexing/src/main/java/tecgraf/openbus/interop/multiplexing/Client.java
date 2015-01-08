package tecgraf.openbus.interop.multiplexing;

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
  public static void main(String[] args) throws Exception {
    Properties props = Utils.readPropertyFile("/test.properties");
    String host = props.getProperty("bus.host.name");
    int port1 = Integer.valueOf(props.getProperty("bus.host.port"));
    int port2 = Integer.valueOf(props.getProperty("bus2.host.port"));
    String domain = "testing";
    Utils.setLibLogLevel(Level.parse(props.getProperty("log.lib", "OFF")));
    int ports[] = { port1, port2 };

    for (int port : ports) {
      ORB orb = ORBInitializer.initORB();
      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection conn = context.connectByAddress(host, port);
      context.setDefaultConnection(conn);
      String login = "interop_multiplexing_java_client";
      conn.loginByPassword(login, login.getBytes(), domain);

      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      serviceProperties[0] =
        new ServiceProperty("openbus.component.interface", HelloHelper.id());
      serviceProperties[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] services =
        context.getOfferRegistry().findServices(serviceProperties);
      for (ServiceOfferDesc offer : services) {
        System.out.println("found offer from "
          + Utils.findProperty(offer.properties, "openbus.offer.entity")
          + " on bus at port " + port);
        org.omg.CORBA.Object obj = offer.service_ref.getFacet(HelloHelper.id());
        Hello hello = HelloHelper.narrow(obj);
        String expected = String.format("Hello %s@%s!", login, conn.busid());
        String sayHello = hello.sayHello();
        assert expected.equals(sayHello) : sayHello;
      }
      conn.logout();
    }
  }
}
