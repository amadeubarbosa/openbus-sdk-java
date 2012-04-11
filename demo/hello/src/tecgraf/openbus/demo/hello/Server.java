package tecgraf.openbus.demo.hello;

import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.PortableServer.POA;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.StandardOpenBus;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.demo.util.Utils.ORBRunThread;
import tecgraf.openbus.demo.util.Utils.ShutdownThread;
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
      RSAPrivateKey privateKey =
        Cryptography.getInstance().readPrivateKey(privateKeyFile);

      OpenBus openbus = StandardOpenBus.getInstance();
      BusORB orb = openbus.initORB(args);

      new ORBRunThread(orb.getORB()).start();
      ShutdownThread shutdown = new ShutdownThread(orb.getORB());
      Runtime.getRuntime().addShutdownHook(shutdown);

      Connection conn = openbus.connect(host, port, orb);
      conn.loginByCertificate(entity, privateKey);
      shutdown.addConnetion(conn);

      POA poa = orb.getRootPOA();
      orb.activateRootPOAManager();
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext context = new ComponentContext(orb.getORB(), poa, id);
      context.addFacet("hello", HelloHelper.id(), new HelloServant(conn));

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "OpenBus Demos");
      conn.offers().registerService(context.getIComponent(), serviceProperties);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
