package tecgraf.openbus.interop.multiplexing.mixed;

import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
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
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port1 = Integer.valueOf(props.getProperty("bus.host.port"));
      int port2 = Integer.valueOf(props.getProperty("bus2.host.port"));

      int ports[] = { port1, port2 };

      for (int port : ports) {
        ORB orb = ORBInitializer.initORB();
        ConnectionManager connections =
          (ConnectionManager) orb
            .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
        Connection conn = connections.createConnection(host, port);
        connections.setDefaultConnection(conn);
        String login = "demo@" + port;
        conn.loginByPassword(login, login.getBytes());

        ServiceProperty[] serviceProperties = new ServiceProperty[2];
        serviceProperties[0] =
          new ServiceProperty("openbus.component.interface", HelloHelper.id());
        serviceProperties[1] =
          new ServiceProperty("offer.domain", "Interoperability Tests");
        ServiceOfferDesc[] services =
          conn.offers().findServices(serviceProperties);
        for (ServiceOfferDesc offer : services) {
          for (ServiceProperty prop : offer.properties) {
            if (prop.name.equals("openbus.offer.entity")) {
              System.out.println("found offer from " + prop.value
                + " on bus at port " + port);
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
        conn.logout();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
