package tecgraf.openbus.interop.simple;

import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;
import tecgraf.openbus.util.Cryptography;

/**
 * Parte servidora do demo Hello
 * 
 * @author Tecgraf
 */
public final class Server {

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

      Properties props = Utils.readPropertyFile("/hello.properties");
      String host = props.getProperty("openbus.host.name");
      int port = Integer.valueOf(props.getProperty("openbus.host.port"));
      String entity = props.getProperty("server.entity.name");
      String privateKeyFile = props.getProperty("server.private.key");
      byte[] privateKey =
        Cryptography.getInstance().readPrivateKey(privateKeyFile);

      ORB orb = ORBInitializer.initORB(args);
      new ORBRunThread(orb).start();
      ShutdownThread shutdown = new ShutdownThread(orb);
      Runtime.getRuntime().addShutdownHook(shutdown);

      ConnectionManager connections =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn = connections.createConnection(host, port);
      connections.setDefaultConnection(conn);
      conn.loginByCertificate(entity, privateKey);
      shutdown.addConnetion(conn);

      POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      poa.the_POAManager().activate();
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext context = new ComponentContext(orb, poa, id);
      context.addFacet("hello", HelloHelper.id(), new HelloServant(conn));

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      conn.offers().registerService(context.getIComponent(), serviceProperties);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
