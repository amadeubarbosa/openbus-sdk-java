package tecgraf.openbus.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.util.Utils;

public class InternalLoginTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static ORB orb;
  private static OpenBusContext manager;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("bus.host.name");
    port = Integer.valueOf(properties.getProperty("bus.host.port"));
    entity = properties.getProperty("user.entity.name");
    password = properties.getProperty("user.password");
    orb = ORBInitializer.initORB();
    manager = (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
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
    assertNull(internal.login());
    LoginInfo login = new LoginInfo("id1", "entity1");
    internal.setLoggedIn(login);
    assertTrue(checkLogin(login, internal.login()));
    internal.setInvalid();
    assertNull(internal.login());
    assertNotNull(internal.invalid());
    assertTrue(checkLogin(login, internal.invalid()));
    LoginInfo login2 = new LoginInfo("id2", "entity2");
    internal.setLoggedIn(login2);
    LoginInfo cplogin = internal.login();
    assertTrue(checkLogin(login2, cplogin));
    assertTrue(!checkLogin(cplogin, login));
    LoginInfo outlogin = internal.setLoggedOut();
    assertTrue(checkLogin(login2, outlogin));
    assertNull(internal.login());
    assertNull(internal.invalid());
  }

  @Test
  public void loginChangeTest() {
    Connection conn = manager.createConnection(host, port);
    InternalLogin internal = new InternalLogin((ConnectionImpl) conn);
    LoginInfo login1 = new LoginInfo("id1", "entity1");
    internal.setLoggedIn(login1);
    LoginInfo info1 = internal.login();
    assertTrue(checkLogin(login1, info1));
    LoginInfo login2 = new LoginInfo("id2", "entity2");
    internal.setLoggedIn(login2);
    LoginInfo info2 = internal.login();
    assertTrue(checkLogin(login1, info1));
    assertTrue(checkLogin(login2, info2));
  }

}
