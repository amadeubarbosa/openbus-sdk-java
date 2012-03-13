package tecgraf.openbus.demo.hello;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
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
      logger.setLevel(Level.FINEST);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.FINEST);
      logger.addHandler(handler);

      ServerProperties props = new ServerProperties();

      BusORB orb = new BusORBImpl(args);

      new ORBRunThread(orb.getORB()).start();
      Runtime.getRuntime().addShutdownHook(new ORBDestroyThread(orb.getORB()));

      Bus bus = orb.getBus(props.getHost(), props.getPort());
      Connection conn = bus.createConnection();
      LoginInfo info =
        conn.loginByCertificate(props.getEntity(), props.getPrivateKey());
      System.out.println(info.id);
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
      conn.getOffers().registerService(context.getIComponent(),
        serviceProperties);
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

  private static class ServerProperties {
    private Properties properties;

    ServerProperties() throws IOException {
      this.properties = new Properties();
      String propertiesFile = "/Hello.properties";
      InputStream propertiesStream =
        Client.class.getResourceAsStream(propertiesFile);
      if (propertiesStream == null) {
        throw new FileNotFoundException(String.format(
          "O arquivo de propriedades %s não foi encontrado", propertiesFile));
      }
      try {
        this.properties.load(propertiesStream);
      }
      finally {
        try {
          propertiesStream.close();
        }
        catch (IOException e) {
          System.err
            .println("Ocorreu um erro ao fechar o arquivo de propriedades");
          e.printStackTrace();
        }
      }
    }

    String getHost() {
      return this.properties.getProperty("openbus.host.name");
    }

    int getPort() {
      String port = this.properties.getProperty("openbus.host.port");
      return Integer.valueOf(port);
    }

    String getEntity() {
      return this.properties.getProperty("server.entity.name");
    }

    RSAPrivateKey getPrivateKey() throws InvalidKeyException,
      NoSuchAlgorithmException, InvalidKeySpecException, IOException {
      String privateKeyFile = this.properties.getProperty("server.private.key");
      return Cryptography.getInstance().readPrivateKey(privateKeyFile);
    }
  }
}
