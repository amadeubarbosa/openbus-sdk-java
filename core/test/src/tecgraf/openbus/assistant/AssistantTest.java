package tecgraf.openbus.assistant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.exception.SCSException;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.util.Utils;

public class AssistantTest {

  private static String host;
  private static int port;
  private static String entity;
  private static byte[] password;
  private static String server;
  private static String privateKeyFile;
  private static OpenBusPrivateKey privateKey;
  private static ORB orb;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    entity = properties.getProperty("entity.name");
    password = properties.getProperty("entity.password").getBytes();
    server = properties.getProperty("server.entity.name");
    privateKeyFile = properties.getProperty("server.private.key");
    privateKey = OpenBusPrivateKey.createPrivateKeyFromFile(privateKeyFile);
    orb = ORBInitializer.initORB();
    Utils.setLogLevel(Level.FINE);
  }

  @Test
  public void createTest() {
    Assistant assist =
      Assistant.createWithPassword(host, port, entity, password);
    Assert.assertNotSame(assist.orb(), orb);
    AuthArgs args = assist.onLoginAuthentication();
    Assert.assertEquals(args.entity, entity);
    Assert.assertTrue(Arrays.equals(args.password, password));
    assist.shutdown();
    assist = Assistant.createWithPrivateKey(host, port, server, privateKey);
    Assert.assertNotSame(assist.orb(), orb);
    args = assist.onLoginAuthentication();
    Assert.assertEquals(args.entity, server);
    Assert.assertSame(args.privkey, privateKey);
    assist.shutdown();
  }

  @Test
  public void reuseORBTest() {
    AssistantParams params = new AssistantParams();
    params.orb = orb;
    Assistant assist =
      Assistant.createWithPassword(host, port, entity, password, params);
    Assert.assertSame(params.orb, orb);
    boolean failed = false;
    try {
      Assistant.createWithPrivateKey(host, port, entity, privateKey, params);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    assist.shutdown();
  }

  @Test
  public void reuseORBbyContextTest() throws Exception {
    AssistantParams params = new AssistantParams();
    params.orb = orb;
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection conn = context.createConnection(host, port);
    context.setDefaultConnection(conn);
    boolean failed = false;
    try {
      Assistant.createWithPassword(host, port, entity, password, params);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    context.setDefaultConnection(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidORBTest() throws IllegalArgumentException {
    String[] args = null;
    Properties props = new Properties();
    props.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    props.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
    AssistantParams params = new AssistantParams();
    params.orb = ORB.init(args, props);
    Assistant.createWithPassword(host, port, entity, password, params);
  }

  @Test
  public void registerTest() throws AdapterInactive, InvalidName, SCSException,
    InterruptedException {
    AssistantParams params = new AssistantParams();
    params.interval = 1;
    Assistant assist =
      Assistant.createWithPrivateKey(host, port, server, privateKey, params);
    ORB orb = assist.orb();
    int index;
    for (index = 0; index < 5; index++) {
      ComponentContext context = Utils.buildComponent(orb);
      List<ServiceProperty> props = new ArrayList<ServiceProperty>();
      props.add(new ServiceProperty("offer.domain", "Assistant Test"));
      props.add(new ServiceProperty("loop.index", Integer.toString(index)));
      assist.registerService(context, props);
    }
    Thread.sleep(params.interval * 3 * 1000);
    ServiceProperty[] search =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Assistant Test") };
    ServiceOfferDesc[] found = assist.findServices(search, 3);
    Assert.assertEquals(index, found.length);
    assist.shutdown();
    assist = Assistant.createWithPrivateKey(host, port, server, privateKey);
    found = assist.findServices(search, 3);
    Assert.assertEquals(0, found.length);
    assist.shutdown();
  }

  @Test
  public void invalidRegisterTest() {

  }

}
