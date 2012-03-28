package tecgraf.openbus.demo.multiplexing.mixed;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionMultiplexer;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.MultiplexedOpenBus;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.hello.HelloHelper;
import tecgraf.openbus.demo.util.Utils;

public class Server {

  public static void main(String[] args) {
    try {
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.FINEST);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.FINEST);
      logger.addHandler(handler);

      Properties properties =
        Utils.readPropertyFile("/multiplexing.properties");
      String host = properties.getProperty("host");
      int port1 = Integer.valueOf(properties.getProperty("port1"));
      int port2 = Integer.valueOf(properties.getProperty("port2"));

      // setup and start the orb
      OpenBus openbus = MultiplexedOpenBus.getInstance();
      BusORB orb1 = openbus.initORB(args);
      new ORBRunThread(orb1.getORB()).start();
      Runtime.getRuntime().addShutdownHook(new ORBDestroyThread(orb1.getORB()));
      orb1.activateRootPOAManager();

      BusORB orb2 = openbus.initORB(args);
      new ORBRunThread(orb2.getORB()).start();
      Runtime.getRuntime().addShutdownHook(new ORBDestroyThread(orb2.getORB()));
      orb2.activateRootPOAManager();

      // connect to the bus
      Connection conn1AtBus1WithOrb1 = openbus.connect(host, port1, orb1);
      Connection conn2AtBus1WithOrb1 = openbus.connect(host, port1, orb1);
      Connection connAtBus2WithOrb1 = openbus.connect(host, port2, orb1);
      Connection connAtBus1WithOrb2 = openbus.connect(host, port1, orb2);

      List<Connection> conns = new ArrayList<Connection>();
      conns.add(conn1AtBus1WithOrb1);
      conns.add(conn2AtBus1WithOrb1);
      conns.add(connAtBus2WithOrb1);

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
      ComponentContext context1 =
        new ComponentContext(orb1.getORB(), orb1.getRootPOA(), id);
      context1.addFacet("hello", HelloHelper.id(), new HelloServant(conns));
      ComponentContext context2 =
        new ComponentContext(orb2.getORB(), orb2.getRootPOA(), id);
      context2.addFacet("hello", HelloHelper.id(), new HelloServant(conns));

      // set incoming connection
      final ConnectionMultiplexer multiplexer =
        (ConnectionMultiplexer) orb1.getORB().resolve_initial_references(
          ConnectionMultiplexer.INITIAL_REFERENCE_ID);
      multiplexer.setIncommingConnection(conn1AtBus1WithOrb1.busid(),
        conn1AtBus1WithOrb1);
      multiplexer.setIncommingConnection(connAtBus2WithOrb1.busid(),
        connAtBus2WithOrb1);

      // login to the bus
      multiplexer.setCurrentConnection(conn1AtBus1WithOrb1);
      conn1AtBus1WithOrb1.loginByPassword("conn1", "conn1".getBytes());
      multiplexer.setCurrentConnection(conn2AtBus1WithOrb1);
      conn2AtBus1WithOrb1.loginByPassword("conn2", "conn2".getBytes());
      multiplexer.setCurrentConnection(connAtBus2WithOrb1);
      connAtBus2WithOrb1.loginByPassword("demo", "demo".getBytes());
      multiplexer.setCurrentConnection(null);
      connAtBus1WithOrb2.loginByPassword("demo", "demo".getBytes());

      Thread thread1 =
        new RegisterThread(conn1AtBus1WithOrb1, multiplexer, context1
          .getIComponent());
      thread1.start();

      Thread thread2 =
        new RegisterThread(conn2AtBus1WithOrb1, multiplexer, context1
          .getIComponent());
      thread2.start();

      multiplexer.setCurrentConnection(connAtBus2WithOrb1);
      connAtBus2WithOrb1.offers().registerService(context1.getIComponent(),
        getProps());
      connAtBus1WithOrb2.offers().registerService(context2.getIComponent(),
        getProps());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ServiceProperty[] getProps() {
    ServiceProperty[] serviceProperties = new ServiceProperty[1];
    serviceProperties[0] = new ServiceProperty("offer.domain", "OpenBus Demos");
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
      try {
        conn.close();
      }
      catch (ServiceFailure e) {
        e.printStackTrace();
      }
      return false;
    }
  }

  private static class RegisterThread extends Thread {

    private Connection conn;
    private ConnectionMultiplexer multiplexer;
    private IComponent component;

    public RegisterThread(Connection conn, ConnectionMultiplexer multiplexer,
      IComponent component) {
      this.conn = conn;
      this.multiplexer = multiplexer;
      this.component = component;
    }

    @Override
    public void run() {
      multiplexer.setCurrentConnection(conn);
      try {
        conn.offers().registerService(component, getProps());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  };

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
