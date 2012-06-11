package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import tecgraf.openbus.core.v2_00.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.util.Utils;

public final class ConnectionManagerTest {
  private static String _hostName;
  private static int _hostPort;
  private static String entity;
  private static String password;

  private static ORB orb;
  private static ConnectionManager _manager;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Properties properties = Utils.readPropertyFile("/test.properties");
    _hostName = properties.getProperty("openbus.host.name");
    _hostPort = Integer.valueOf(properties.getProperty("openbus.host.port"));
    entity = properties.getProperty("entity.name");
    password = properties.getProperty("entity.password");
    orb = ORBInitializer.initORB();
    _manager =
      (ConnectionManager) orb
        .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
  }

  @Test
  public void ORBTest() {
    assertNotNull(_manager.orb());
  }

  @Test
  public void createConnectionTest() {
    // cria conexão válida
    Connection valid = _manager.createConnection(_hostName, _hostPort);
    assertNotNull(valid);
    // tenta criar conexão com hosts inválidos
    Connection invalid = null;
    try {
      invalid = _manager.createConnection("", _hostPort);
    }
    catch (Exception e) {
    }
    finally {
      assertNull(invalid);
    }
    try {
      invalid = _manager.createConnection(_hostName, -1);
    }
    catch (Exception e) {
    }
    finally {
      assertNull(invalid);
    }
  }

  @Test
  public void getDispatcherTest() {
    Connection conn = _manager.createConnection(_hostName, _hostPort);
    Connection conn2 = _manager.createConnection(_hostName, _hostPort);
    _manager.setDefaultConnection(conn);
    assertNull(_manager.getDispatcher(conn.busid()));
    _manager.setDispatcher(conn2);
    assertEquals(_manager.getDispatcher(conn.busid()), conn2);
    _manager.clearDispatcher(conn.busid());
    assertNull(_manager.getDispatcher(conn2.busid()));
    _manager.setDefaultConnection(null);
    assertNull(_manager.getDispatcher(conn.busid()));
  }

  @Test
  public void clearDispatcherTest() {
    Connection conn = _manager.createConnection(_hostName, _hostPort);
    Connection conn2 = _manager.createConnection(_hostName, _hostPort);
    Connection removed = _manager.clearDispatcher(conn.busid());
    assertNull(removed);
    _manager.setDefaultConnection(null);
    _manager.setDispatcher(conn2);
    removed = _manager.clearDispatcher(conn.busid());
    assertEquals(removed, conn2);
    _manager.setDefaultConnection(null);
  }

  @Test
  public void setDispatcherTest() {
    Connection conn = _manager.createConnection(_hostName, _hostPort);
    Connection conn2 = _manager.createConnection(_hostName, _hostPort);
    assertNull(_manager.getDispatcher(conn.busid()));
    _manager.setDefaultConnection(conn);
    _manager.setRequester(conn);
    assertNull(_manager.getDispatcher(conn.busid()));
    _manager.setDispatcher(conn2);
    assertEquals(_manager.getDispatcher(conn.busid()), conn2);
    _manager.clearDispatcher(conn.busid());
    _manager.setDefaultConnection(null);
    _manager.setRequester(null);
    assertNull(_manager.getDispatcher(conn.busid()));
  }

  @Test
  public void defaultConnectionTest() {
    _manager.setDefaultConnection(null);
    Connection conn = _manager.createConnection(_hostName, _hostPort);
    assertNull(_manager.getDefaultConnection());
    _manager.setDispatcher(conn);
    _manager.setRequester(conn);
    assertNull(_manager.getDefaultConnection());
    _manager.setDefaultConnection(conn);
    assertEquals(_manager.getDefaultConnection(), conn);
    _manager.setDefaultConnection(null);
    _manager.clearDispatcher(conn.busid());
    _manager.setRequester(null);
  }

  @Test
  public void requesterTest() throws AccessDenied, AlreadyLoggedIn,
    ServiceFailure {
    Connection conn = _manager.createConnection(_hostName, _hostPort);
    assertNull(_manager.getRequester());
    _manager.setDispatcher(conn);
    _manager.setDefaultConnection(conn);
    assertNull(_manager.getRequester());
    _manager.setRequester(conn);
    assertEquals(_manager.getRequester(), conn);
    _manager.setDefaultConnection(null);
    _manager.clearDispatcher(conn.busid());
    _manager.setRequester(null);

    // tentativa de chamada sem threadrequester setado
    conn.loginByPassword(entity, password.getBytes());
    assertNull(_manager.getRequester());
    boolean failed = false;
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("a", "b") };
    try {
      conn.offers().findServices(props);
    }
    catch (NO_PERMISSION e) {
      failed = true;
      if (e.minor != NoLoginCode.value) {
        fail("A exceção deveria ser NO_PERMISSION com minor code NoLoginCode. Minor code recebido: "
          + e.minor);
      }
    }
    catch (Exception e) {
      fail("A exceção deveria ser NO_PERMISSION com minor code NoLoginCode. Exceção recebida: "
        + e);
    }
    assertTrue(failed);
    // tentativa com threadrequester setado
    _manager.setRequester(conn);
    try {
      conn.offers().findServices(props);
    }
    catch (Exception e) {
      fail("A chamada com ThreadRequester setado deveria ser bem-sucedida. Exceção recebida: "
        + e);
    }
  }
}
