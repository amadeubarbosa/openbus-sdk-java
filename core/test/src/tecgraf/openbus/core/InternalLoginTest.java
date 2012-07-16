package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.InternalLogin.LoginStatus;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.Utils;

public class InternalLoginTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static ORB orb;
  private static ConnectionManager manager;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    entity = properties.getProperty("entity.name");
    password = properties.getProperty("entity.password");
    orb = ORBInitializer.initORB();
    manager =
      (ConnectionManager) orb
        .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
  }

  private boolean checkLogin(LoginInfo login1, LoginInfo login2) {
    if ((login1.id != login2.id) || (login1.entity != login2.entity)) {
      return false;
    }
    return true;
  }

  @Test
  public void statusTest() {
    Connection conn = manager.createConnection(host, port);
    InternalLogin internal = new InternalLogin((ConnectionImpl) conn);
    assertNull(internal.getLogin());
    LoginInfo login = new LoginInfo("id1", "entity1");
    LoginStatus t0 = internal.getStatus();
    assertEquals(t0, LoginStatus.loggedOut);
    internal.setLoggedIn(login);
    LoginStatus t1 = internal.getStatus();
    assertEquals(t1, LoginStatus.loggedIn);
    assertTrue(t0 != t1);
    assertNotSame(login, internal.getLogin());
    assertTrue(checkLogin(login, internal.getLogin()));
    internal.setInvalid();
    LoginStatus t2 = internal.getStatus();
    assertEquals(t2, LoginStatus.invalid);
    assertNotSame(login, internal.getLogin());
    assertTrue(checkLogin(login, internal.getLogin()));
    LoginInfo login2 = new LoginInfo("id2", "entity2");
    internal.setLoggedIn(login2);
    LoginStatus t3 = internal.getStatus();
    assertEquals(t3, LoginStatus.loggedIn);
    LoginInfo cplogin = internal.getLogin();
    assertNotSame(login2, cplogin);
    assertTrue(checkLogin(login2, cplogin));
    assertTrue(!checkLogin(cplogin, login));
    LoginInfo outlogin = internal.setLoggedOut();
    assertNotSame(login2, outlogin);
    assertTrue(checkLogin(login2, outlogin));
    assertSame(cplogin, outlogin);
    assertNull(internal.getLogin());
    LoginStatus t4 = internal.getStatus();
    assertEquals(t0, LoginStatus.loggedOut);
    assertEquals(t1, LoginStatus.loggedIn);
    assertEquals(t2, LoginStatus.invalid);
    assertEquals(t3, LoginStatus.loggedIn);
    assertEquals(t4, LoginStatus.loggedOut);
  }

  @Test
  public void loginChangeTest() {
    Connection conn = manager.createConnection(host, port);
    InternalLogin internal = new InternalLogin((ConnectionImpl) conn);
    LoginInfo login1 = new LoginInfo("id1", "entity1");
    internal.setLoggedIn(login1);
    LoginInfo info1 = internal.getLogin();
    assertTrue(checkLogin(login1, info1));
    LoginInfo login2 = new LoginInfo("id2", "entity2");
    internal.setLoggedIn(login2);
    LoginInfo info2 = internal.getLogin();
    assertTrue(checkLogin(login1, info1));
    assertTrue(checkLogin(login2, info2));
  }

}
