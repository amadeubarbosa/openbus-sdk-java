package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.util.Utils;

public final class ConnectionTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String serverEntity;
  private static String password;
  private static ORB orb;
  private static ConnectionManager manager;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    entity = properties.getProperty("entity.name");
    serverEntity = properties.getProperty("server.entity.name");
    password = properties.getProperty("entity.password");
    orb = ORBInitializer.initORB();
    manager =
      (ConnectionManager) orb
        .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
  }

  private Connection createConnection() {
    Connection conn = manager.createConnection(host, port);
    manager.setDefaultConnection(conn);
    return conn;
  }

  @Test
  public void orbTest() throws Exception {
    assertEquals(orb, manager.orb());
  }
}
