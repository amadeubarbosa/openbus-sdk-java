package tecgraf.openbus.demo.hello;

import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.Bus;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.BusORBImpl;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;
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

      Properties props = Utils.readPropertyFile("/Hello.properties");
      String host = props.getProperty("openbus.host.name");
      int port = Integer.valueOf(props.getProperty("openbus.host.port"));
      String entity = props.getProperty("server.entity.name");
      String privateKeyFile = props.getProperty("server.private.key");
      RSAPrivateKey privateKey =
        Cryptography.getInstance().readPrivateKey(privateKeyFile);

      BusORB orb = new BusORBImpl(args);

      new ORBRunThread(orb.getORB()).start();
      Runtime.getRuntime().addShutdownHook(new ORBDestroyThread(orb.getORB()));

      Bus bus = orb.getBus(host, port);
      Connection conn = bus.createConnection();
      conn.loginByCertificate(entity, privateKey);
      LoginInfo info = conn.login();
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");

      POA poa = orb.getRootPOA();
      POAManager manager = poa.the_POAManager();
      manager.activate();

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

  private static class ORBRunThread extends Thread {
    private ORB orb;

    ORBRunThread(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void run() {
      this.orb.run();
    }
  }

  private static class ORBDestroyThread extends Thread {
    private ORB orb;

    ORBDestroyThread(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void run() {
      this.orb.shutdown(true);
      this.orb.destroy();
    }
  }
}
