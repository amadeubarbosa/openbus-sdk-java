package tecgraf.openbus.demo.multiplexing.byorb;

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
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.hello.HelloHelper;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.demo.util.Utils.ORBRunThread;
import tecgraf.openbus.demo.util.Utils.ShutdownThread;

public class Server {

  public static void main(String[] args) {
    try {
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      Properties properties =
        Utils.readPropertyFile("/multiplexing.properties");
      String host = properties.getProperty("host");
      int port = Integer.valueOf(properties.getProperty("port1"));

      // setup and start the orb
      ORB orb1 = ORBInitializer.initORB(args);
      new ORBRunThread(orb1).start();
      ShutdownThread shutdown1 = new ShutdownThread(orb1);
      Runtime.getRuntime().addShutdownHook(shutdown1);

      ORB orb2 = ORBInitializer.initORB(args);
      new ORBRunThread(orb2).start();
      ShutdownThread shutdown2 = new ShutdownThread(orb2);
      Runtime.getRuntime().addShutdownHook(shutdown2);

      // connect to the bus
      ConnectionManager connections1 =
        (ConnectionManager) orb1
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn1 = connections1.createConnection(host, port);
      connections1.setDefaultConnection(conn1);

      ConnectionManager connections2 =
        (ConnectionManager) orb2
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn2 = connections2.createConnection(host, port);
      connections2.setDefaultConnection(conn2);

      // setup action on login termination
      conn1.onInvalidLoginCallback(new Callback(conn1, "conn1"));
      conn2.onInvalidLoginCallback(new Callback(conn2, "conn2"));

      // create service SCS component
      POA poa1 = POAHelper.narrow(orb1.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext context1 = new ComponentContext(orb1, poa1, id);
      context1.addFacet("hello", HelloHelper.id(), new HelloServant(conn1));

      POA poa2 = POAHelper.narrow(orb2.resolve_initial_references("RootPOA"));
      poa2.the_POAManager().activate();
      ComponentContext context2 = new ComponentContext(orb2, poa2, id);
      context2.addFacet("hello", HelloHelper.id(), new HelloServant(conn2));

      // login to the bus
      conn1.loginByPassword("conn1", "conn1".getBytes());
      shutdown1.addConnetion(conn1);
      conn2.loginByPassword("conn2", "conn2".getBytes());
      shutdown2.addConnetion(conn2);

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "OpenBus Demos");
      conn1.offers().registerService(context1.getIComponent(),
        serviceProperties);
      conn2.offers().registerService(context1.getIComponent(),
        serviceProperties);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static class Callback implements InvalidLoginCallback {

    private String name;
    private Connection conn;

    public Callback(Connection conn, String name) {
      this.name = name;
      this.conn = conn;
    }

    @Override
    public boolean invalidLogin(Connection conn, LoginInfo login) {
      System.out.println("login terminated: " + name);
      return false;
    }
  }

}
