package tecgraf.openbus.interop.multiplexing;

import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  private static final Logger logger = Logger.getLogger(Client.class.getName());

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
    Utils.setTestLogLevel(Level.parse(props.getProperty("log.test", "OFF")));
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

      int noffers;
      if (port == port1) {
        noffers = 3;
      }
      else {
        noffers = 1;
      }
      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      serviceProperties[0] =
        new ServiceProperty("openbus.component.interface", HelloHelper.id());
      serviceProperties[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      List<ServiceOfferDesc> services =
        Utils.findOffer(context.getOfferRegistry(), serviceProperties, noffers,
          10, 1);
      for (ServiceOfferDesc offer : services) {
        logger.fine(String.format("found offer from %s on bus at port %d",
          Utils.findProperty(offer.properties, "openbus.offer.entity"), port));
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
