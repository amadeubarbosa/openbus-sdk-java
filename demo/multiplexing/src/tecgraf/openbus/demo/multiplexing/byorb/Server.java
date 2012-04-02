package tecgraf.openbus.demo.multiplexing.byorb;

import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
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
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      Properties properties =
        Utils.readPropertyFile("/multiplexing.properties");
      String host = properties.getProperty("host");
      int port1 = Integer.valueOf(properties.getProperty("port1"));

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
      Connection conn1 = openbus.connect(host, port1, orb1);
      Connection conn2 = openbus.connect(host, port1, orb2);

      // setup action on login termination
      conn1.onInvalidLoginCallback(new Callback(conn1, "conn1"));
      conn2.onInvalidLoginCallback(new Callback(conn2, "conn2"));

      // create service SCS component
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext context1 =
        new ComponentContext(orb1.getORB(), orb1.getRootPOA(), id);
      context1.addFacet("hello", HelloHelper.id(), new HelloServant(conn1));
      ComponentContext context2 =
        new ComponentContext(orb2.getORB(), orb2.getRootPOA(), id);
      context2.addFacet("hello", HelloHelper.id(), new HelloServant(conn2));

      // login to the bus
      conn1.loginByPassword("conn1", "conn1".getBytes());
      conn2.loginByPassword("conn2", "conn2".getBytes());

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
      try {
        conn.close();
      }
      catch (ServiceFailure e) {
        e.printStackTrace();
      }
      return false;
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
