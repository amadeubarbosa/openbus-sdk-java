package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.interfaces.RSAPrivateKey;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import scs.core.ComponentContext;
import tecgraf.openbus.*;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.util.Builder;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.Utils;

public final class ConnectionTest {

  //private static String host;
  //private static int port;
  private static Object busref;
  private static String entity;
  private static byte[] password;
  private static String domain;
  private static String system;
  private static RSAPrivateKey systemKey;
  private static RSAPrivateKey systemWrongKey;
  private static String systemWrongName;
  private static String admin;
  private static byte[] adminpwd;
  private static ORB orb;
  private static OpenBusContextImpl context;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Configs configs = Configs.readConfigsFile();
    Utils.setLibLogLevel(configs.log);
    //host = configs.bushost;
    //port = configs.busport;
    entity = configs.user;
    password = configs.password;
    domain = configs.domain;
    system = configs.system;
    systemKey = crypto.readKeyFromFile(configs.syskey);
    systemWrongName = configs.wrongsystem;
    systemWrongKey = crypto.readKeyFromFile(configs.wrongkey);
    admin = configs.admin;
    adminpwd = configs.admpsw;
    orb =
      ORBInitializer.initORB(null, Utils.readPropertyFile(configs.orbprops));
    busref = orb.string_to_object(new String(Utils.readFile(configs.busref)));
    context = (OpenBusContextImpl) orb.resolve_initial_references
      ("OpenBusContext");
  }

  @After
  public void afterEachTest() {
    context.setCurrentConnection(null);
    context.setDefaultConnection(null);
    context.onCallDispatch(null);
    context.exitChain();
  }

  @Test
  public void orbTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    assertNotNull(conn.orb());
    assertEquals(orb, context.orb());
  }

  @Test
  public void offerRegistryNoLoginTest() {
    Connection conn = context.connectByReference(busref);
    try {
      OfferRegistry registryService = context.getOfferRegistry();
      ServiceProperty[] props =
        new ServiceProperty[] { new ServiceProperty("a", "b") };
      registryService.findServices(props);
    }
    catch (NO_PERMISSION e) {
      assertEquals(e.minor, NoLoginCode.value);
    }
    catch (ServiceFailure e) {
      fail(e.message);
    }
  }

  @Test
  public void busIdTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    assertNull(conn.busid());
    conn.loginByPassword(entity, entity.getBytes(), domain);
    assertNotNull(conn.busid());
    assertFalse(conn.busid().isEmpty());
    assertTrue(conn.logout());
    assertNull(conn.busid());
    assertNull(conn.login());
  }

  @Test
  public void loginTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    assertNull(conn.login());
    conn.loginByPassword(entity, entity.getBytes(), domain);
    assertNotNull(conn.login());
    conn.logout();
    assertNull(conn.login());
  }

  @Test
  public void loginByPasswordTest() throws Exception {
    Connection conn = context.connectByReference(busref);

    // entidade errada
    boolean failed = false;
    try {
      conn.loginByPassword("", password, domain);
    }
    catch (AccessDenied e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
    }
    assertTrue("O login com entidade vazia foi bem-sucedido.", failed);

    // senha errada
    failed = false;
    try {
      conn.loginByPassword("invalid-entity-1", new byte[0], domain);
    }
    catch (AccessDenied e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
    }
    assertTrue("O login com senha vazia foi bem-sucedido.", failed);

    // login válido
    assertNull(conn.login());
    conn.loginByPassword(entity, password, domain);
    assertNotNull(conn.login());
    conn.logout();
    assertNull(conn.login());

    // login repetido
    failed = false;
    try {
      conn.loginByPassword(entity, password, domain);
      conn.loginByPassword(entity, password, domain);
    }
    catch (AlreadyLoggedIn e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AlreadyLoggedInException. Exceção recebida: "
        + e);
    }
    assertTrue("O login com entidade já autenticada foi bem-sucedido.", failed);
    conn.logout();
    assertNull(conn.login());
  }

  @Test
  public void loginByPrivateKeyTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    // entidade sem certificado cadastrado
    boolean failed = false;
    try {
      conn.loginByPrivateKey(systemWrongName, systemKey);
    }
    catch (MissingCertificate e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser MissingCertificate. Exceção recebida: " + e);
    }
    assertTrue(
      "O login de entidade sem certificado cadastrado foi bem-sucedido.",
      failed);

    // chave privada corrompida
    failed = false;
    try {
      RSAPrivateKey key =
        Cryptography.getInstance().readKeyFromBytes(new byte[0]);
      conn.loginByPrivateKey(system, key);
    }
    catch (Exception e) {
      failed = true;
    }
    assertTrue("O login de entidade com chave corrompida foi bem-sucedido.",
      failed);

    // chave privada inválida
    failed = false;
    try {
      conn.loginByPrivateKey(system, systemWrongKey);
    }
    catch (AccessDenied e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser WrongPrivateKeyException. Exceção recebida: "
        + e);
    }
    assertTrue("O login de entidade com chave errada foi bem-sucedido.", failed);

    // login válido
    assertNull(conn.login());
    conn.loginByPrivateKey(system, systemKey);
    assertNotNull(conn.login());
    assertTrue(conn.logout());
    assertNull(conn.login());

    // login repetido
    failed = false;
    try {
      conn.loginByPrivateKey(system, systemKey);
      conn.loginByPrivateKey(system, systemKey);
    }
    catch (AlreadyLoggedIn e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AlreadyLoggedInException. Exceção recebida: "
        + e);
    }
    assertTrue("O login com entidade já autenticada foi bem-sucedido.", failed);
    assertTrue(conn.logout());
    assertNull(conn.login());
  }

  @Test
  public void loginBySharedAuthenticationFakeTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    Connection conn2 = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);

    // segredo errado
    boolean failed = false;
    try {
      context.setCurrentConnection(conn);
      SharedAuthSecretImpl secret =
        (SharedAuthSecretImpl) conn.startSharedAuth();
      context.setCurrentConnection(null);
      conn2.loginByCallback(() -> new AuthArgs(new SharedAuthSecretImpl(conn
        .busid(), secret.attempt(), null, new byte[0], context)));
    }
    catch (AccessDenied e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
    }
    assertTrue("O login com segredo errado foi bem-sucedido.", failed);
    conn.logout();
    assertNull(conn.login());
    context.setCurrentConnection(null);
  }

  @Test
  public void loginBySharedAuthenticationTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    Connection conn2 = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);

    // login válido
    assertNull(conn2.login());
    context.setCurrentConnection(conn);
    final SharedAuthSecret secret = conn.startSharedAuth();
    context.setCurrentConnection(null);
    conn2.loginByCallback(() -> new AuthArgs(secret));
    assertNotNull(conn2.login());
    conn2.logout();
    assertNull(conn2.login());
    context.setCurrentConnection(null);

    // login repetido
    boolean failed = false;
    try {
      context.setCurrentConnection(conn);
      final SharedAuthSecret secret2 = conn.startSharedAuth();
      context.setCurrentConnection(null);
      conn2.loginByCallback(() -> new AuthArgs(secret2));
      assertNotNull(conn2.login());
      conn2.loginByCallback(() -> new AuthArgs(secret2));
    }
    catch (AlreadyLoggedIn e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser AlreadyLoggedInException. Exceção recebida: "
        + e);
    }
    assertTrue("O login com entidade já autenticada foi bem-sucedido.", failed);
    conn2.logout();
    assertNull(conn2.login());
    conn.logout();
    assertNull(conn.login());
    context.setCurrentConnection(null);
  }

  @Test
  public void logoutTest() throws Exception {
    final Connection conn = context.connectByReference(busref);
    assertTrue(conn.logout());
    conn.loginByPassword(entity, password, domain);
    final String busId = conn.busid();
    context.onCallDispatch((context1, busid, loginId, object_id, operation) -> {
      if (busId.equals(busid)) {
        return conn;
      }
      return null;
    });
    assertTrue(conn.logout());
    assertNull(conn.busid());
    assertNull(conn.login());
    boolean failed = false;
    try {
      context.setCurrentConnection(conn);
      context.getOfferRegistry().findServices(new ServiceProperty[0]);
    }
    catch (NO_PERMISSION e) {
      failed = true;
      if (e.minor != NoLoginCode.value) {
        fail("A exceção é NO_PERMISSION mas o minor code não é NoLoginCode. Minor code recebido: "
          + e.minor);
      }
    }
    catch (Exception e) {
      fail("A exceção deveria ser NO_PERMISSION. Exceção recebida: " + e);
    }
    assertTrue("Uma busca sem login foi bem-sucedida.", failed);
  }

  @Test
  public void onInvalidLoginCallbackTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    String id = conn.login().id;

    Connection adminconn = context.connectByReference(busref);
    adminconn.loginByPassword(admin, adminpwd, domain);
    try {
      context.setCurrentConnection(adminconn);
      context.getLoginRegistry().invalidateLogin(id);
      context.setCurrentConnection(null);
      adminconn.logout();

      context.setCurrentConnection(conn);
      LoginRegistry logins = context.getLoginRegistry();
      int validity = logins.getLoginValidity(id);
      context.setCurrentConnection(null);
      assertTrue(validity <= 0);
      assertNotNull(conn.login());
    }
    finally {
      context.setCurrentConnection(null);
    }
  }

  @Test
  public void registerAndFindTest() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    Connection conn2 = context.connectByReference(busref);
    try {
      String sys1 = "test_entity_registration_first";
      String sys2 = "test_entity_registration_second";
      conn1.loginByPrivateKey(sys1, systemKey);
      assertNotNull(conn1.login());
      conn2.loginByPrivateKey(sys2, systemKey);
      assertNotNull(conn2.login());
      ServiceProperty[] props =
        new ServiceProperty[] { new ServiceProperty("offer.domain", "testing") };
      ComponentContext comp1 =
        Builder.buildTestCallerChainInspectorComponent(context);

      // conn1
      context.setDefaultConnection(conn1);
      context.getOfferRegistry().registerService(comp1.getIComponent(), props);
      // conn2
      context.setCurrentConnection(conn2);
      context.getOfferRegistry().registerService(comp1.getIComponent(), props);
      context.setCurrentConnection(null);

      props =
        new ServiceProperty[] { new ServiceProperty("offer.domain", "testing"),
            new ServiceProperty("openbus.offer.entity", sys1) };
      ServiceOfferDesc[] services =
        context.getOfferRegistry().findServices(props);
      assertEquals(1, services.length);
      // invert property list order
      props =
        new ServiceProperty[] {
            new ServiceProperty("openbus.offer.entity", sys1),
            new ServiceProperty("offer.domain", "testing") };
      services = context.getOfferRegistry().findServices(props);
      assertEquals(1, services.length);
    }
    finally {
      context.setDefaultConnection(null);
      conn1.logout();
      conn2.logout();
    }
  }

  @Test
  public void logoutOnInvalidLoginCallbackTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    String id = conn.login().id;

    Connection adminconn = context.connectByReference(busref);
    adminconn.loginByPassword(admin, adminpwd, domain);
    try {
      context.setCurrentConnection(adminconn);
      boolean ok = context.getLoginRegistry().invalidateLogin(id);
      int validity = context.getLoginRegistry().getLoginValidity(id);
      context.setCurrentConnection(null);
      adminconn.logout();
      assertTrue(ok);
      assertTrue(validity <= 0);

      boolean logout = conn.logout();
      assertTrue(logout);
      assertNull(conn.login());
    }
    finally {
      context.setCurrentConnection(null);
    }
  }
}
