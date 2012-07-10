package tecgraf.openbus.interop.multiplexing.mixed;

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
      int port1 = Integer.valueOf(properties.getProperty("port1"));
      int port2 = Integer.valueOf(properties.getProperty("port2"));

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
      ConnectionManager connections2 =
        (ConnectionManager) orb2
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);

      Connection conn1AtBus1WithOrb1 =
        connections1.createConnection(host, port1);
      Connection conn2AtBus1WithOrb1 =
        connections1.createConnection(host, port1);
      Connection connAtBus2WithOrb1 =
        connections1.createConnection(host, port2);
      Connection connAtBus1WithOrb2 =
        connections2.createConnection(host, port1);

      List<Connection> conns = new ArrayList<Connection>();
      conns.add(conn1AtBus1WithOrb1);
      conns.add(connAtBus2WithOrb1);
      conns.add(connAtBus1WithOrb2);

      // setup action on login termination
      conn1AtBus1WithOrb1.onInvalidLoginCallback(new Callback(
        conn1AtBus1WithOrb1, "conn1AtBus1WithOrb1"));
      conn2AtBus1WithOrb1.onInvalidLoginCallback(new Callback(
        conn2AtBus1WithOrb1, "conn2AtBus1WithOrb1"));
      connAtBus2WithOrb1.onInvalidLoginCallback(new Callback(
        connAtBus2WithOrb1, "connAtBus2WithOrb1"));
      connAtBus1WithOrb2.onInvalidLoginCallback(new Callback(
        connAtBus1WithOrb2, "connAtBus1WithOrb2"));

      // create service SCS component
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      POA poa1 = POAHelper.narrow(orb1.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();
      ComponentContext context1 = new ComponentContext(orb1, poa1, id);
      context1.addFacet("hello", HelloHelper.id(), new HelloServant(conns));
      POA poa2 = POAHelper.narrow(orb2.resolve_initial_references("RootPOA"));
      poa2.the_POAManager().activate();
      ComponentContext context2 = new ComponentContext(orb2, poa2, id);
      context2.addFacet("hello", HelloHelper.id(), new HelloServant(conns));

      // set incoming connection
      connections1.setDispatcher(conn1AtBus1WithOrb1);
      connections1.setDispatcher(connAtBus2WithOrb1);

      // login to the bus
      connections1.setRequester(conn1AtBus1WithOrb1);
      conn1AtBus1WithOrb1.loginByPassword("conn1", "conn1".getBytes());
      shutdown1.addConnetion(conn1AtBus1WithOrb1);
      connections1.setRequester(conn2AtBus1WithOrb1);
      conn2AtBus1WithOrb1.loginByPassword("conn2", "conn2".getBytes());
      shutdown1.addConnetion(conn2AtBus1WithOrb1);
      connections1.setRequester(connAtBus2WithOrb1);
      connAtBus2WithOrb1.loginByPassword("demo", "demo".getBytes());
      shutdown1.addConnetion(connAtBus2WithOrb1);
      connections1.setRequester(null);
      connAtBus1WithOrb2.loginByPassword("demo", "demo".getBytes());
      shutdown2.addConnetion(connAtBus1WithOrb2);

      Thread thread1 =
        new RegisterThread(conn1AtBus1WithOrb1, connections1, context1
          .getIComponent());
      thread1.start();

      Thread thread2 =
        new RegisterThread(conn2AtBus1WithOrb1, connections1, context1
          .getIComponent());
      thread2.start();

      connections1.setRequester(connAtBus2WithOrb1);
      connAtBus2WithOrb1.offers().registerService(context1.getIComponent(),
        getProps());

      connections2.setRequester(connAtBus1WithOrb2);
      connections2.setDispatcher(connAtBus1WithOrb2);
      connAtBus1WithOrb2.offers().registerService(context2.getIComponent(),
        getProps());
      connections2.setRequester(null);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ServiceProperty[] getProps() {
    ServiceProperty[] serviceProperties = new ServiceProperty[1];
    serviceProperties[0] = new ServiceProperty("offer.domain", "Interoperability Tests");
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
    public boolean invalidLogin(Connection conn, LoginInfo login) {
      System.out.println("login terminated: " + name);
      return false;
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
