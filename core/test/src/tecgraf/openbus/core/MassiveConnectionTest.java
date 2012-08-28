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
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.Utils;

public class MassiveConnectionTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static ORB orb;
  private static OpenBusContext connections;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    orb = ORBInitializer.initORB();
    connections =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
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

    private OpenBusContext context;
    private String entity;

    public ConnectThread(OpenBusContext multiplexer, int id) {
      this.context = multiplexer;
      this.entity = "Task-" + id;
    }

    @Override
    public void run() {
      try {
        Connection conn = context.createConnection(host, port);
        conn.loginByPassword(entity, entity.getBytes());
        context.setCurrentConnection(conn);
        context.getOfferRegistry().getServices();
        context.getOfferRegistry().getServices();
        context.setCurrentConnection(null);
        conn.logout();
      }
      catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }
  };
}
