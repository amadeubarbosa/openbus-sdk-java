package tecgraf.openbus.interop.multiplexing.bythread;

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

      Properties properties =
        Utils.readPropertyFile("/multiplexing.properties");
      BusAddress bus1 =
        new BusAddress(properties.getProperty("host1"), Integer
          .valueOf(properties.getProperty("port1")));
      BusAddress bus2 =
        new BusAddress(properties.getProperty("host2"), Integer
          .valueOf(properties.getProperty("port2")));

      BusAddress buses[] = { bus1, bus2 };

      for (BusAddress busAddr : buses) {
        ORB orb = ORBInitializer.initORB();
        ConnectionManager manager =
          (ConnectionManager) orb
            .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
        Connection conn =
          manager.createConnection(busAddr.hostname, busAddr.port);
        manager.setDefaultConnection(conn);
        String login = "interop@" + busAddr.hostname + ":" + busAddr.port;
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
                + " on bus at " + busAddr.hostname + ":" + busAddr.port);
            }
          }
          org.omg.CORBA.Object obj =
            offer.service_ref.getFacet(HelloHelper.id());
          Hello hello = HelloHelper.narrow(obj);
          hello.sayHello();
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
