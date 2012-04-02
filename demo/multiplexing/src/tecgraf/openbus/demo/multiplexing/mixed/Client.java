package tecgraf.openbus.demo.multiplexing.mixed;

import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.StandardOpenBus;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.hello.Hello;
import tecgraf.openbus.demo.hello.HelloHelper;
import tecgraf.openbus.demo.util.Utils;

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
      int port1 = Integer.valueOf(properties.getProperty("port1"));
      int port2 = Integer.valueOf(properties.getProperty("port2"));
      int ports[] = { port1, port2 };

      for (int port : ports) {
        OpenBus openbus = StandardOpenBus.getInstance();
        Connection conn = openbus.connect(host, port);
        String login = "demo@" + port;
        conn.loginByPassword(login, login.getBytes());

        ServiceProperty[] serviceProperties = new ServiceProperty[2];
        serviceProperties[0] =
          new ServiceProperty("openbus.component.facet", "hello");
        serviceProperties[1] =
          new ServiceProperty("offer.domain", "OpenBus Demos");
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
        conn.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
