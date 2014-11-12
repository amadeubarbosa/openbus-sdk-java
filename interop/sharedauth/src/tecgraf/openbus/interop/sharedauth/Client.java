package tecgraf.openbus.interop.sharedauth;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.security.Cryptography;

/**
 * Demo Single Sign On.
 * 
 * @author Tecgraf
 */
public final class Client {

  /**
   * Fun��o main.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = "interop_sharedauth_java_client";
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      ORB orb = ORBInitializer.initORB();

      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection connection = context.createConnection(host, port);
      context.setDefaultConnection(connection);

      connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET));
      SharedAuthSecret secret = connection.startSharedAuth();
      byte[] encoded = context.encodeSharedAuthSecret(secret);
      File file = new File("sharedauth.dat");
      FileOutputStream fstream = new FileOutputStream(file);
      fstream.write(encoded);
      fstream.close();

      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      serviceProperties[0] =
        new ServiceProperty("openbus.component.interface", HelloHelper.id());
      serviceProperties[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] services =
        context.getOfferRegistry().findServices(serviceProperties);

      if (services.length < 1) {
        System.err.println("O servidor do demo Hello n�o foi encontrado");
        connection.logout();
        System.exit(1);
      }
      if (services.length > 1) {
        System.out.println("Foram encontrados v�rios servidores do demo Hello");
      }

      for (ServiceOfferDesc offerDesc : services) {

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
    }
  }

}
