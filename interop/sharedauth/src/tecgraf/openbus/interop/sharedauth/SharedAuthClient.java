package tecgraf.openbus.interop.sharedauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcessHelper;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;

public class SharedAuthClient {
  public static void main(String[] args) {
    try {
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      Properties properties = Utils.readPropertyFile("/sharedauth.properties");
      String host = properties.getProperty("openbus.host.name");
      int port = Integer.valueOf(properties.getProperty("openbus.host.port"));

      ORB orb = ORBInitializer.initORB();

      ConnectionManager manager =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection connection = manager.createConnection(host, port);
      manager.setDefaultConnection(connection);

      LoginProcess process = readLoginProcess(properties, orb);
      byte[] secret = readSecret(properties);

      connection.loginBySharedAuth(process, secret);

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

  private static byte[] readSecret(Properties properties) throws IOException {
    File file = new File(properties.getProperty("secretFile"));
    FileInputStream fstream = new FileInputStream(file);
    byte[] secret = new byte[(int) file.length()];
    fstream.read(secret);
    fstream.close();
    return secret;
  }

  private static LoginProcess readLoginProcess(Properties properties, ORB orb)
    throws IOException {
    FileInputStream fstream =
      new FileInputStream(properties.getProperty("loginFile"));
    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
    String loginProcessIOR = br.readLine();
    br.close();
    return LoginProcessHelper.narrow(orb.string_to_object(loginProcessIOR));
  }
}
