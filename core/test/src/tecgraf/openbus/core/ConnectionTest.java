package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
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
    assertEquals(orb, createConnection().orb());
  }

  @Test
  public void offerRegistryTest() {
    Connection conn = createConnection();
    try {
      OfferRegistry registryService = conn.offers();
      ServiceProperty[] props =
        new ServiceProperty[] { new ServiceProperty("a", "b") };
      registryService.findServices(props);
    }
    catch (NO_PERMISSION e) {
    }
    catch (ServiceFailure e) {
      fail(e.message);
    }
  }

  @Test
  public void busIdTest() throws Exception {
    Connection conn = createConnection();
    assertNotNull(conn.busid());
    assertFalse(conn.busid().isEmpty());
  }

  @Test
  public void loginByPasswordTest() throws Exception {
    Connection conn = createConnection();

    // entidade errada
    boolean failed = false;
    try {
      conn.loginByPassword("", password.getBytes());
    }
    catch (AccessDenied e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
    }

    if (!failed) {
      fail("O login com entidade vazia foi bem-sucedido.");
    }

    // senha errada
    failed = false;
    try {
      conn.loginByPassword(entity, new byte[0]);
    }
    catch (AccessDenied e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
    }
    if (!failed) {
      fail("O login com senha vazia foi bem-sucedido.");
    }

    // login válido
    assertNull(conn.login());
    conn.loginByPassword(entity, password.getBytes());
    assertNotNull(conn.login());
    conn.logout();
    assertNull(conn.login());

    // login repetido
    failed = false;
    try {
      conn.loginByPassword(entity, password.getBytes());
      conn.loginByPassword(entity, password.getBytes());
    }
    catch (AlreadyLoggedIn e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AlreadyLoggedInException. Exceção recebida: "
        + e);
    }
    if (!failed) {
      fail("O login com entidade já autenticada foi bem-sucedido.");
    }
    conn.logout();
  }
}
