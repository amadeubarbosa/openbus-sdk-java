package tecgraf.openbus.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.Utils;

public class InternalLoginTest {

  private static String host;
  private static int port;
  private static ORB orb;
  private static OpenBusContext context;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Configs configs = Configs.readConfigsFile();
    Utils.setLibLogLevel(configs.log);
    host = configs.bushost;
    port = configs.busport;
    orb =
      ORBInitializer.initORB(null, Utils.readPropertyFile(configs.orbprops));
    context = (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
  }

  private boolean checkLogin(LoginInfo login1, LoginInfo login2) {
    if ((login1.id != login2.id) || (login1.entity != login2.entity)) {
      return false;
    }
    return true;
  }

  @Test
  public void statusTest() {
    Connection conn = context.connectByAddress(host, port);
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
    Connection conn = context.connectByAddress(host, port);
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
