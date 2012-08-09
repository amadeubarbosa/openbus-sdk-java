package demo;

import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.demo.util.Utils.ORBRunThread;
import tecgraf.openbus.demo.util.Utils.ShutdownThread;
import tecgraf.openbus.util.Cryptography;

/**
 * Parte servidora do demo Hello
 * 
 * @author Tecgraf
 */
public final class HelloServer {

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
      String entity = "interop_hello_java_server";
      String privateKeyFile = "admin/InteropHello.key";
      byte[] privateKey =
        Cryptography.getInstance().readPrivateKey(privateKeyFile);
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      ORB orb = ORBInitializer.initORB(args);
      new ORBRunThread(orb).start();
      ShutdownThread shutdown = new ShutdownThread(orb);
      Runtime.getRuntime().addShutdownHook(shutdown);

      ConnectionManager manager =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn = manager.createConnection(host, port);
      manager.setDefaultConnection(conn);
      conn.loginByCertificate(entity, privateKey);
      conn.onInvalidLoginCallback(new HelloInvalidLoginCallback(entity,
        privateKey));

      shutdown.addConnetion(conn);

      POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      poa.the_POAManager().activate();
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext context = new ComponentContext(orb, poa, id);
      context.addFacet("Hello", HelloHelper.id(), new HelloImpl(conn));

      ServiceProperty[] serviceProperties =
        new ServiceProperty[] { new ServiceProperty("offer.domain",
          "Interoperability Tests") };
      OfferRegistry registry = conn.offers();
      ServiceOffer offer =
        registry.registerService(context.getIComponent(), serviceProperties);
      shutdown.addOffer(offer);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
