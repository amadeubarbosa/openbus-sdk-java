package tecgraf.openbus.interop.sharedauth;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.Any;
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
   * Função main.
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

      ConnectionManager manager =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection connection = manager.createConnection(host, port);
      manager.setDefaultConnection(connection);

      connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET));
      OctetSeqHolder secret = new OctetSeqHolder();
      LoginProcess process = connection.startSharedAuth(secret);

      EncodedSharedAuth data = new EncodedSharedAuth(process, secret.value);
      Any any = orb.create_any();
      EncodedSharedAuthHelper.insert(any, data);
      byte[] encoded = Utils.getCodec(orb).encode_value(any);
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
