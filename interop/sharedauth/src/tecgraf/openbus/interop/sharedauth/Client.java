package tecgraf.openbus.interop.sharedauth;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.util.Cryptography;

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
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      Properties properties = Utils.readPropertyFile("/sharedauth.properties");
      String host = properties.getProperty("openbus.host.name");
      int port = Integer.valueOf(properties.getProperty("openbus.host.port"));
      String entity = properties.getProperty("entity.name");
      String password = properties.getProperty("entity.password");

      ORB orb = ORBInitializer.initORB();

      ConnectionManager manager =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection connection = manager.createConnection(host, port);
      manager.setDefaultConnection(connection);

      connection.loginByPassword(entity, password
        .getBytes(Cryptography.CHARSET));
      OctetSeqHolder secret = new OctetSeqHolder();
      LoginProcess process = connection.startSharedAuth(secret);

      writeLoginProcess(properties, process, orb);
      writeSecret(properties, secret);

      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      serviceProperties[0] =
        new ServiceProperty("openbus.component.interface", HelloHelper.id());
      serviceProperties[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] services =
        connection.offers().findServices(serviceProperties);

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

        hello.sayHello();
      }

      connection.logout();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void writeSecret(Properties properties, OctetSeqHolder secret)
    throws IOException {
    File file = new File(properties.getProperty("secretFile"));
    FileOutputStream fstream = new FileOutputStream(file);
    fstream.write(secret.value);
    fstream.close();
  }

  private static void writeLoginProcess(Properties properties,
    LoginProcess process, ORB orb) throws IOException {
    FileOutputStream fstream =
      new FileOutputStream(properties.getProperty("loginFile"));
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fstream));
    bw.write(orb.object_to_string(process));
    bw.close();

  }
}