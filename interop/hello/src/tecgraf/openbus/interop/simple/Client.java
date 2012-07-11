package tecgraf.openbus.interop.simple;

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
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.util.Cryptography;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class Client {
  /**
   * Função principal.
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

      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      serviceProperties[0] =
        new ServiceProperty("openbus.component.interface", HelloHelper.id());
      serviceProperties[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] services =
        connection.offers().findServices(serviceProperties);

      if (services.length < 1) {
        System.err.println("O servidor do demo Hello não foi encontrado");
        connection.logout();
        System.exit(1);
      }
      if (services.length > 1) {
        System.out.println("Foram encontrados vários servidores do demo Hello");
      }

      for (ServiceOfferDesc offerDesc : services) {

        org.omg.CORBA.Object helloObj =
          offerDesc.service_ref.getFacetByName("Hello");
        if (helloObj == null) {
          System.out
            .println("Não foi possível encontrar uma faceta com esse nome.");
          continue;
        }

        Hello hello = HelloHelper.narrow(helloObj);
        if (hello == null) {
          System.out.println("Faceta encontrada não implementa Hello.");
          continue;
        }

        hello.sayHello();
      }

      connection.logout();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
