package tecgraf.openbus.interop.simple;

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
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.util.Cryptography;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class Client {
  /**
   * Fun��o principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) {
    try {
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      Properties properties = Utils.readPropertyFile("/hello.properties");
      String host = properties.getProperty("openbus.host.name");
      int port = Integer.valueOf(properties.getProperty("openbus.host.port"));
      String entity = properties.getProperty("entity.name");
      String serverEntity = properties.getProperty("server.entity.name");
      String password = properties.getProperty("entity.password");

      ORB orb = ORBInitializer.initORB();
      ConnectionManager connections =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection connection = connections.createConnection(host, port);
      connections.setDefaultConnection(connection);

      connection.loginByPassword(entity, password
        .getBytes(Cryptography.CHARSET));

      ServiceProperty[] serviceProperties = new ServiceProperty[3];
      serviceProperties[0] =
        new ServiceProperty("openbus.offer.entity", serverEntity);
      serviceProperties[1] =
        new ServiceProperty("openbus.component.facet", "hello");
      serviceProperties[2] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] services =
        connection.offers().findServices(serviceProperties);

      if (services.length == 1) {
        org.omg.CORBA.Object obj =
          services[0].service_ref.getFacetByName("hello");

        Hello hello = HelloHelper.narrow(obj);
        hello.sayHello();
      }
      else {
        if (services.length == 0) {
          System.err.println("O servidor do demo Hello n�o foi encontrado");
        }
        else {
          System.err
            .println("Foram encontrados v�rios servidores do demo Hello");
        }
      }

      connection.logout();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}