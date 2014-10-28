package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;

import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidPropertyValue;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.util.Utils;

public final class ConnectionTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static String serverEntity;
  private static String privateKeyFile;
  private static RSAPrivateKey privateKey;
  private static RSAPrivateKey wrongPrivateKey;
  private static String entityWithoutCert;
  private static ORB orb;
  private static OpenBusContext context;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    entity = properties.getProperty("entity.name");
    password = properties.getProperty("entity.password");
    serverEntity = properties.getProperty("server.entity.name");
    privateKeyFile = properties.getProperty("server.private.key");
    privateKey = crypto.readKeyFromFile(privateKeyFile);
    entityWithoutCert = properties.getProperty("entity.withoutcert");
    String wrongPrivateKeyFile = properties.getProperty("wrongkey");
    wrongPrivateKey = crypto.readKeyFromFile(wrongPrivateKeyFile);
    orb = ORBInitializer.initORB();
    context = (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
  }

  @Test
  public void orbTest() throws Exception {
    Connection conn = context.createConnection(host, port);
    assertNotNull(conn.orb());
    assertEquals(orb, context.orb());
  }

  @Test
  public void offerRegistryTest() {
    Connection conn = context.createConnection(host, port);
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
    Connection conn = context.createConnection(host, port);
    assertNull(conn.busid());
    conn.loginByPassword(entity, entity.getBytes());
    assertNotNull(conn.busid());
    assertFalse(conn.busid().isEmpty());
    assertTrue(conn.logout());
    assertNull(conn.busid());
    assertNull(conn.login());
  }

  @Test
  public void loginTest() throws Exception {
    Connection conn = context.createConnection(host, port);
    assertNull(conn.login());
    conn.loginByPassword(entity, entity.getBytes());
    assertNotNull(conn.login());
    conn.logout();
    assertNull(conn.login());
  }

  @Test
  public void accessKeyPropTest() throws Exception {
    Properties properties = new Properties();
    properties.put(OpenBusProperty.ACCESS_KEY.getKey(), privateKeyFile);
    Connection conn = context.createConnection(host, port, properties);
    assertNull(conn.login());
    conn.loginByPassword(entity, entity.getBytes());
    assertNotNull(conn.login());
    conn.logout();
    assertNull(conn.login());
  }

  @Test(expected = InvalidPropertyValue.class)
  public void invalidAccessKeyPropTest() throws Exception {
    Properties properties = new Properties();
    properties.put(OpenBusProperty.ACCESS_KEY.getKey(),
      "/invalid/path/to/access.key");
    Connection conn = context.createConnection(host, port, properties);
  }

  @Test
  public void invalidHostPortLoginTest() throws Exception {
    Connection conn = context.createConnection("unknown-host", port);
    assertNull(conn.login());
    try {
      conn.loginByPassword(entity, entity.getBytes());
    }
    catch (TRANSIENT e) {
      // erro esperado
    }
    catch (Exception e) {
      fail("A exceção deveria ser TRANSIENT. Exceção recebida: " + e);
    }
    assertNull(conn.login());
    // chutando uma porta inválida
    conn = context.createConnection(host, port + 111);
    assertNull(conn.login());
    try {
      conn.loginByPassword(entity, entity.getBytes());
    }
    catch (TRANSIENT e) {
      // erro esperado
    }
    catch (Exception e) {
      fail("A exceção deveria ser TRANSIENT. Exceção recebida: " + e);
    }
    assertNull(conn.login());
  }

  @Test
  public void loginByPasswordTest() throws Exception {
    Connection conn = context.createConnection(host, port);

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
    assertTrue("O login com entidade vazia foi bem-sucedido.", failed);

    // senha errada
    failed = false;
    try {
      conn.loginByPassword("invalid-entity-1", new byte[0]);
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
    assertTrue("O login com entidade já autenticada foi bem-sucedido.", failed);
    conn.logout();
    assertNull(conn.login());
  }

  @Test
  public void loginByCertificateTest() throws Exception {
    Connection conn = context.createConnection(host, port);
    // entidade sem certificado cadastrado
    boolean failed = false;
    try {
      conn.loginByCertificate(entityWithoutCert, privateKey);
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
      conn.loginByCertificate(serverEntity, key);
    }
    catch (Exception e) {
      // TODO verificar a exceção específica?
      failed = true;
    }
    assertTrue("O login de entidade com chave corrompida foi bem-sucedido.",
      failed);

    // chave privada inválida
    failed = false;
    try {
      conn.loginByCertificate(serverEntity, wrongPrivateKey);
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
    conn.loginByCertificate(serverEntity, privateKey);
    assertNotNull(conn.login());
    assertTrue(conn.logout());
    assertNull(conn.login());

    // login repetido
    failed = false;
    try {
      conn.loginByCertificate(serverEntity, privateKey);
      conn.loginByCertificate(serverEntity, privateKey);
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
  public void loginBySharedAuthenticationTest() throws Exception {
    Connection conn = context.createConnection(host, port);
    Connection conn2 = context.createConnection(host, port);
    conn.loginByPassword(entity, password.getBytes());

    // segredo errado
    boolean failed = false;
    OctetSeqHolder secret = new OctetSeqHolder();
    LoginProcess login;

    try {
      context.setCurrentConnection(conn);
      login = conn.startSharedAuth(secret);
      context.setCurrentConnection(null);
      conn2.loginBySharedAuth(login, new byte[0]);
    }
    catch (AccessDenied e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser WrongSecretException. Exceção recebida: " + e);
    }

    assertTrue("O login com segredo errado foi bem-sucedido.", failed);

    // login válido
    assertNull(conn2.login());
    context.setCurrentConnection(conn);
    login = conn.startSharedAuth(secret);
    context.setCurrentConnection(null);
    conn2.loginBySharedAuth(login, secret.value);
    assertNotNull(conn2.login());
    conn2.logout();
    assertNull(conn2.login());
    context.setCurrentConnection(null);

    // login repetido
    failed = false;
    try {
      context.setCurrentConnection(conn);
      login = conn.startSharedAuth(secret);
      context.setCurrentConnection(null);
      conn2.loginBySharedAuth(login, secret.value);
      assertNotNull(conn2.login());
      conn2.loginBySharedAuth(login, secret.value);
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
    final Connection conn = context.createConnection(host, port);
    assertFalse(conn.logout());
    conn.loginByPassword(entity, password.getBytes());
    final String busId = conn.busid();
    context.onCallDispatch(new CallDispatchCallback() {

      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        if (busId.equals(busid)) {
          return conn;
        }
        return null;
      }
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
    Connection conn = context.createConnection(host, port);
    assertNull(conn.onInvalidLoginCallback());
    InvalidLoginCallback callback = new InvalidLoginCallbackMock();
    conn.onInvalidLoginCallback(callback);
    assertEquals(callback, conn.onInvalidLoginCallback());
  }

}
