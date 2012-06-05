package tecgraf.openbus.intereop.multiplexing.byorb;

import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.intereop.simple.Hello;
import tecgraf.openbus.intereop.simple.HelloHelper;
import tecgraf.openbus.intereop.util.Utils;

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
      String host = properties.getProperty("host");
      int port = Integer.valueOf(properties.getProperty("port1"));

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
        new ServiceProperty("openbus.component.facet", "hello");
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
        org.omg.CORBA.Object obj = offer.service_ref.getFacetByName("hello");
        Hello hello = HelloHelper.narrow(obj);
        hello.sayHello();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
