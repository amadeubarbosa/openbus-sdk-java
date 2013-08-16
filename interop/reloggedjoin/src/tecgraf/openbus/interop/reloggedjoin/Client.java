package tecgraf.openbus.interop.reloggedjoin;

import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.security.Cryptography;

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
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = "interop_reloggedjoin_java_client";
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      ORB orb = ORBInitializer.initORB();
      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection connection = context.createConnection(host, port);
      context.setDefaultConnection(connection);

      connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET));

      ServiceProperty[] serviceProperties =
        new ServiceProperty[] {
            new ServiceProperty("reloggedjoin.role", "proxy"),
            new ServiceProperty("offer.domain", "Interoperability Tests") };
      ServiceOfferDesc[] services =
        context.getOfferRegistry().findServices(serviceProperties);

      if (services.length < 1) {
        System.err.println("O proxy do demo Hello n�o foi encontrado");
        connection.logout();
        System.exit(1);
      }
      if (services.length > 1) {
        System.out.println("Foram encontrados v�rios proxies do demo Hello: "
          + services.length);
      }

      for (ServiceOfferDesc offerDesc : services) {
        String found =
          Utils.findProperty(offerDesc.properties, "openbus.offer.entity");
        System.out.println("Entidade encontrada: " + found);
        org.omg.CORBA.Object helloObj =
          offerDesc.service_ref.getFacetByName("Hello");
        if (helloObj == null) {
          System.out
            .println("N�o foi poss�vel encontrar uma faceta com esse nome.");
          continue;
        }

        Hello hello = HelloHelper.narrow(helloObj);
        if (hello == null) {
          System.out.println("Faceta encontrada n�o implementa Hello.");
          continue;
        }
        String expected = "Hello " + entity + "!";
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

      connection.logout();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
