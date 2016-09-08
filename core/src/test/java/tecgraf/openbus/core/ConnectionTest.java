package tecgraf.openbus.core;

import com.google.common.collect.ArrayListMultimap;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import scs.core.ComponentContext;
import tecgraf.openbus.Connection;
import tecgraf.openbus.LocalOffer;
import tecgraf.openbus.LoginCallback;
import tecgraf.openbus.OfferRegistry;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.util.Builder;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.Utils;

import java.security.interfaces.RSAPrivateKey;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
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
    context.currentConnection(null);
    context.defaultConnection(null);
    context.onCallDispatch(null);
    context.exitChain();
  }

  @Test
  public void orbTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    assertNotNull(conn.ORB());
    assertEquals(orb, context.ORB());
  }

  @Test
  public void offerRegistryNoLoginTest() {
    Connection conn = context.connectByReference(busref);
    try {
      OfferRegistry registryService = conn.offerRegistry();
      ArrayListMultimap<String, String> props = ArrayListMultimap.create();
      props.put("a", "b");
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
      conn2.loginByCallback(new TestLoginCallback(conn, true));
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
  }

  @Test
  public void loginBySharedAuthenticationTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    Connection conn2 = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);

    // login válido
    assertNull(conn2.login());
    LoginCallback cb = new TestLoginCallback(conn, false);
    conn2.loginByCallback(cb);
    assertNotNull(conn2.login());
    conn2.logout();
    assertNull(conn2.login());

    // login repetido
    boolean failed = false;
    try {
      conn2.loginByCallback(cb);
      assertNotNull(conn2.login());
      conn2.loginByCallback(cb);
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
      conn.offerRegistry().findServices(ArrayListMultimap.create());
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
    assertNotNull(conn);
    conn.loginByPassword(entity, password, domain);
    String id = conn.login().id;

    Connection adminconn = context.connectByReference(busref);
    adminconn.loginByPassword(admin, adminpwd, domain);
    adminconn.loginRegistry().invalidateLogin(id);
    int validity = adminconn.loginRegistry().loginValidity(id);
    assertTrue(validity <= 0);
    adminconn.logout();
    // faz chamada para refazer login
    conn.loginRegistry().entityLogins(entity);
    validity = conn.loginRegistry().loginValidity(conn.login().id);
    assertTrue(validity > 0);
    conn.logout();
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
      ArrayListMultimap<String, String> props = ArrayListMultimap.create();
      props.put("offer.domain", "testing");
      ComponentContext comp1 =
        Builder.buildTestCallerChainInspectorComponent(context);

      context.onCallDispatch((context1, busid, loginId, object_id, operation)
        -> conn1);
      // conn1
      LocalOffer local = conn1.offerRegistry().registerService(comp1
        .getIComponent(), props);
      local.remoteOffer(10000);
      context.onCallDispatch((context1, busid, loginId, object_id, operation)
        -> conn2);
      // conn2
      LocalOffer local2 = conn2.offerRegistry().registerService(comp1
        .getIComponent(), props);
      local2.remoteOffer(10000);

      props.clear();
      props.put("offer.domain", "testing");
      props.put("openbus.offer.entity", sys1);
      List<RemoteOffer> services = conn1.offerRegistry().findServices(props);
      assertEquals(1, services.size());
      // invert property list order
      props.clear();
      props.put("openbus.offer.entity", sys1);
      props.put("offer.domain", "testing");
      services = conn1.offerRegistry().findServices(props);
      assertEquals(1, services.size());
    }
    finally {
      conn1.logout();
      conn2.logout();
    }
  }

  private class TestLoginCallback implements LoginCallback {
    private final Connection conn;
    private final boolean fake;

    public TestLoginCallback(Connection conn, boolean fake) {
      this.conn = conn;
      this.fake = fake;
    }

    @Override
    public AuthArgs authenticationArguments() {
      SharedAuthSecret secret;
      try {
        secret = conn.startSharedAuth();
      } catch (ServiceFailure serviceFailure) {
        return null;
      }
      if (fake) {
        secret = new SharedAuthSecretImpl(conn.busid(), (
          (SharedAuthSecretImpl)secret).attempt(), null, new byte[0], context);
      }
      return new AuthArgs(secret);
    }
  }
}
