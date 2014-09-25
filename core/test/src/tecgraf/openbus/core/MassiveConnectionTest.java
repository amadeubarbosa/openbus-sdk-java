package tecgraf.openbus.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import scs.core.ComponentContext;
import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.util.Utils;

public class MassiveConnectionTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static ORB orb;
  private static OpenBusContext context;
  private static int LOOP_SIZE = 20;
  private static int THREAD_POOL_SIZE = 6;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    orb = ORBInitializer.initORB();
    context = (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
  }

  @Test
  public void massiveTest() throws Exception {
    String entity = "dispatcher";
    final Connection conn = context.connectByAddress(host, port);
    conn.loginByPassword(entity, entity.getBytes());
    context.onCallDispatch(new CallDispatchCallback() {
      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        return conn;
      }
    });
    ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    AtomicBoolean failed = new AtomicBoolean(false);
    for (int i = 0; i < LOOP_SIZE; i++) {
      threadPool.execute(new ConnectThread(context, i, failed));
    }
    threadPool.shutdown();
    try {
      threadPool.awaitTermination(2, TimeUnit.MINUTES);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    finally {
      conn.logout();
    }
    assertFalse(failed.get());
  }

  private static class ConnectThread implements Runnable {

    private OpenBusContext context;
    private String entity;
    private AtomicBoolean failed;

    public ConnectThread(OpenBusContext multiplexer, int id,
      AtomicBoolean failed) {
      this.context = multiplexer;
      this.entity = "Task-" + id;
      this.failed = failed;
    }

    @Override
    public void run() {
      try {
        final Connection conn = context.connectByAddress(host, port);
        conn.loginByPassword(entity, entity.getBytes());
        context.setCurrentConnection(conn);
        ComponentContext component = Utils.buildComponent(orb);
        ServiceProperty[] props =
          new ServiceProperty[] {
              new ServiceProperty("offer.domain", "Massive Test"),
              new ServiceProperty("thread.id", entity) };
        ServiceOffer offer =
          context.getOfferRegistry().registerService(component.getIComponent(),
            props);
        ServiceOfferDesc[] services =
          context.getOfferRegistry().findServices(props);
        if (services.length != 1) {
          failed.set(true);
        }
        if (services[0] == null) {
          failed.set(true);
        }
        offer.remove();
        context.setCurrentConnection(null);
        conn.logout();
      }
      catch (Exception e) {
        failed.set(true);
      }
    }
  }
}
