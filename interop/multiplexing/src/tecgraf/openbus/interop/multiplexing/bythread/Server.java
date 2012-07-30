package tecgraf.openbus.interop.multiplexing.bythread;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;
import tecgraf.openbus.util.Cryptography;

public class Server {

  public static void main(String[] args) {
    try {
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      String host2 = props.getProperty("bus2.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      int port2 = Integer.valueOf(props.getProperty("bus2.host.port"));
      String entityPrefix = "interop_multiplexing_java_conn";
      String privateKeyFile = "admin/InteropMultiplexing.key";

      Cryptography crypto = Cryptography.getInstance();
      byte[] privateKey = crypto.readPrivateKey(privateKeyFile);

      String entity1 = entityPrefix + "1";
      String entity2 = entityPrefix + "2";
      String entity3 = entityPrefix + "3";

      // setup and start the orb
      ORB orb = ORBInitializer.initORB(args);
      new ORBRunThread(orb).start();
      ShutdownThread shutdown = new ShutdownThread(orb);
      Runtime.getRuntime().addShutdownHook(shutdown);

      // connect to the bus
      ConnectionManager manager =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);

      Connection conn1AtBus1 = manager.createConnection(host, port);
      Connection conn2AtBus1 = manager.createConnection(host, port);
      Connection connAtBus2 = manager.createConnection(host2, port2);

      List<Connection> conns = new ArrayList<Connection>();
      conns.add(conn1AtBus1);
      conns.add(conn2AtBus1);
      conns.add(connAtBus2);

      POA poa1 = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();

      // create service SCS component
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext context1 = new ComponentContext(orb, poa1, id);
      context1.addFacet("hello", HelloHelper.id(), new HelloServant(conns));

      // login to the bus
      conn1AtBus1.loginByCertificate(entity1, privateKey);
      conn2AtBus1.loginByCertificate(entity2, privateKey);
      connAtBus2.loginByCertificate(entity3, privateKey);

      shutdown.addConnetion(conn1AtBus1);
      shutdown.addConnetion(conn2AtBus1);
      shutdown.addConnetion(connAtBus2);

      // Set incoming connections
      manager.setDispatcher(conn1AtBus1);
      manager.setDispatcher(connAtBus2);

      Thread thread1 =
        new RegisterThread(conn1AtBus1, manager, context1.getIComponent());
      thread1.start();
      conn1AtBus1.onInvalidLoginCallback(new Callback(conn1AtBus1,
        "conn1AtBus1"));

      Thread thread2 =
        new RegisterThread(conn2AtBus1, manager, context1.getIComponent());
      thread2.start();
      conn2AtBus1.onInvalidLoginCallback(new Callback(conn2AtBus1,
        "conn2AtBus1"));

      manager.setRequester(connAtBus2);
      connAtBus2.offers().registerService(context1.getIComponent(), getProps());
      connAtBus2.onInvalidLoginCallback(new Callback(connAtBus2, "connAtBus2"));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ServiceProperty[] getProps() {
    ServiceProperty[] serviceProperties = new ServiceProperty[1];
    serviceProperties[0] =
      new ServiceProperty("offer.domain", "Interoperability Tests");
    return serviceProperties;
  }

  private static class Callback implements InvalidLoginCallback {

    private String name;
    private Connection conn;

    public Callback(Connection conn, String name) {
      this.name = name;
      this.conn = conn;
    }

    @Override
    public void invalidLogin(Connection conn, LoginInfo login) {
      System.out.println("login terminated: " + name);
    }
  }

  private static class RegisterThread extends Thread {

    private Connection conn;
    private ConnectionManager multiplexer;
    private IComponent component;

    public RegisterThread(Connection conn, ConnectionManager multiplexer,
      IComponent component) {
      this.conn = conn;
      this.multiplexer = multiplexer;
      this.component = component;
    }

    @Override
    public void run() {
      multiplexer.setRequester(conn);
      try {
        conn.offers().registerService(component, getProps());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  };

}
