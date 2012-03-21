package tecgraf.openbus.demo.multiplexing;

import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
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
      int port2 = Integer.valueOf(properties.getProperty("port2"));

      BusORB orb1 = OpenBus.initORB(args);
      orb1.activateRootPOAManager();
      BusORB orb2 = OpenBus.initORB(args);
      orb2.activateRootPOAManager();

      Connection conn1AtBus1WithOrb1 = OpenBus.connect(host, port1, orb1);
      Connection conn2AtBus1WithOrb1 = OpenBus.connect(host, port1, orb1);
      Connection connAtBus2WithOrb1 = OpenBus.connect(host, port2, orb1);
      Connection connAtBus1WithOrb2 = OpenBus.connect(host, port1, orb2);

      conn1AtBus1WithOrb1.onInvalidLoginCallback(new Callback(
        conn1AtBus1WithOrb1, "conn1AtBus1WithOrb1"));
      conn2AtBus1WithOrb1.onInvalidLoginCallback(new Callback(
        conn2AtBus1WithOrb1, "conn2AtBus1WithOrb1"));
      connAtBus2WithOrb1.onInvalidLoginCallback(new Callback(
        connAtBus2WithOrb1, "connAtBus2WithOrb1"));
      connAtBus1WithOrb2.onInvalidLoginCallback(new Callback(
        connAtBus1WithOrb2, "connAtBus1WithOrb2"));

      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext context1 =
        new ComponentContext(orb1.getORB(), orb1.getRootPOA(), id);
      // TODO continuar demo
      //context1.addFacet("hello", HelloHelper.id(), new HelloServant(conn));
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
}
