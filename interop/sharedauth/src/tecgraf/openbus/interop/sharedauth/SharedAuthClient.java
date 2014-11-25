package tecgraf.openbus.interop.sharedauth;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_1.services.access_control.LoginProcessHelper;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;

public class SharedAuthClient {
  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      ORB orb = ORBInitializer.initORB();

      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection connection = context.connectByAddress(host, port);
      context.setDefaultConnection(connection);

      File file = new File("sharedauth.dat");
      FileInputStream fstream = new FileInputStream(file);
      byte[] encoded = new byte[(int) file.length()];
      int read = fstream.read(encoded);
      if (read != file.length()) {
        System.err.println("Erro de leitura!");
        return;
      }
      fstream.close();
      SharedAuthSecret secret = context.decodeSharedAuthSecret(encoded);
      connection.loginBySharedAuth(secret);

      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      serviceProperties[0] =
        new ServiceProperty("openbus.component.interface", HelloHelper.id());
      serviceProperties[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] services =
        context.getOfferRegistry().findServices(serviceProperties);

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

        String sayHello = hello.sayHello();
        System.out.println("Received: " + sayHello);
      }

      connection.logout();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
