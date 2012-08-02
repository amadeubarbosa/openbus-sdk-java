package tecgraf.openbus.core;

import static org.junit.Assert.fail;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.Utils;

public class MassiveConnectionTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static ORB orb;
  private static ConnectionManager connections;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    orb = ORBInitializer.initORB();
    connections =
      (ConnectionManager) orb
        .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
  }

  @Test
  public void massiveTest() throws InterruptedException {
    ExecutorService threadPool = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 100; i++) {
      threadPool.execute(new ConnectThread(connections, i));
    }
    threadPool.shutdown();
    try {
      threadPool.awaitTermination(2, TimeUnit.MINUTES);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  private static class ConnectThread implements Runnable {

    private ConnectionManager connections;
    private String entity;

    public ConnectThread(ConnectionManager multiplexer, int id) {
      this.connections = multiplexer;
      this.entity = "Task-" + id;
    }

    @Override
    public void run() {
      try {
        Connection conn = connections.createConnection(host, port);
        conn.loginByPassword(entity, entity.getBytes());
        connections.setRequester(conn);
        conn.offers().getServices();
        conn.offers().getServices();
        connections.setRequester(null);
        conn.logout();
      }
      catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }
  };
}
