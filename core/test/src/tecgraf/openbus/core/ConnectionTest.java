package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.v2_00.OctetSeqHolder;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_00.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_00.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.CorruptedPrivateKey;
import tecgraf.openbus.exception.WrongPrivateKey;
import tecgraf.openbus.exception.WrongSecret;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.Utils;

public final class ConnectionTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static String serverEntity;
  private static String privateKeyFile;
  private static byte[] privateKey;
  private static byte[] wrongPrivateKey;
  private static String entityWithoutCert;
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
    serverEntity = properties.getProperty("server.entity.name");
    privateKeyFile = properties.getProperty("server.private.key");
    privateKey = crypto.readPrivateKey(privateKeyFile);
    entityWithoutCert = properties.getProperty("entity.withoutcert");
    String wrongPrivateKeyFile = properties.getProperty("wrongkey");
    wrongPrivateKey = crypto.readPrivateKey(wrongPrivateKeyFile);
    orb = ORBInitializer.initORB();
    manager =
      (ConnectionManager) orb
        .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
  }

  @Test
  public void orbTest() throws Exception {
    Connection conn = manager.createConnection(host, port);
    assertNotNull(conn.orb());
    assertEquals(orb, manager.orb());
  }

  @Test
  public void offerRegistryTest() {
    Connection conn = manager.createConnection(host, port);
    try {
      OfferRegistry registryService = conn.offers();
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
    Connection conn = manager.createConnection(host, port);
    assertNull(conn.busid());
    conn.loginByPassword(entity, entity.getBytes());
    assertNotNull(conn.busid());
    assertFalse(conn.busid().isEmpty());
    assertTrue(conn.logout());
    assertNull(conn.login());
  }

  @Test
  public void loginTest() throws Exception {

    Connection conn = manager.createConnection(host, port);
    assertNull(conn.login());
    conn.loginByPassword(entity, entity.getBytes());
    assertNotNull(conn.login());
    conn.logout();
    assertNull(conn.login());

  }

  @Test
  public void loginByPasswordTest() throws Exception {
    Connection conn = manager.createConnection(host, port);

    // entidade errada
    boolean failed = false;
    try {
      conn.loginByPassword("", password.getBytes());
    }
    catch (AccessDenied e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exce��o deveria ser AccessDenied. Exce��o recebida: " + e);
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
      fail("A exce��o deveria ser AccessDenied. Exce��o recebida: " + e);
    }
    if (!failed) {
      fail("O login com senha vazia foi bem-sucedido.");
    }

    // login v�lido
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
      fail("A exce��o deveria ser AlreadyLoggedInException. Exce��o recebida: "
        + e);
    }
    if (!failed) {
      fail("O login com entidade j� autenticada foi bem-sucedido.");
    }
    conn.logout();
  }

  @Test
  public void loginByCertificateTest() throws Exception {
    Connection conn = manager.createConnection(host, port);
    // entidade sem certificado cadastrado
    boolean failed = false;
    try {
      conn.loginByCertificate(entityWithoutCert, privateKey);
    }
    catch (MissingCertificate e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exce��o deveria ser MissingCertificate. Exce��o recebida: " + e);
    }
    assertTrue(
      "O login de entidade sem certificado cadastrado foi bem-sucedido.",
      failed);

    // chave privada corrompida
    failed = false;
    try {
      conn.loginByCertificate(serverEntity, new byte[0]);
    }
    catch (CorruptedPrivateKey e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exce��o deveria ser CorruptedPrivateKeyException. Exce��o recebida: "
        + e);
    }
    assertTrue("O login de entidade com chave corrompida foi bem-sucedido.",
      failed);

    // chave privada inv�lida
    failed = false;
    try {
      conn.loginByCertificate(serverEntity, wrongPrivateKey);
    }
    catch (WrongPrivateKey e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exce��o deveria ser WrongPrivateKeyException. Exce��o recebida: "
        + e);
    }
    assertTrue("O login de entidade com chave errada foi bem-sucedido.", failed);

    // login v�lido
    assertNull(conn.login());
    conn.loginByCertificate(serverEntity, privateKey);
    assertNotNull(conn.login());

    conn.logout();
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
      fail("A exce��o deveria ser AlreadyLoggedInException. Exce��o recebida: "
        + e);
    }
    assertTrue("O login com entidade j� autenticada foi bem-sucedido.", failed);
    conn.logout();
  }

  @Test
  public void singleSignOnTest() throws Exception {
    Connection conn = manager.createConnection(host, port);
    Connection conn2 = manager.createConnection(host, port);
    manager.setRequester(conn);
    conn.loginByPassword(entity, password.getBytes());

    // segredo errado
    boolean failed = false;
    OctetSeqHolder secret = new OctetSeqHolder();
    LoginProcess login;

    try {
      login = conn.startSingleSignOn(secret);
      manager.setRequester(conn2);
      conn2.loginBySingleSignOn(login, new byte[0]);
    }
    catch (WrongSecret e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exce��o deveria ser WrongSecretException. Exce��o recebida: " + e);
    }

    assertTrue("O login com segredo errado foi bem-sucedido.", failed);

    // login v�lido
    assertNull(conn2.login());
    manager.setRequester(conn);
    login = conn.startSingleSignOn(secret);
    manager.setRequester(conn2);
    conn2.loginBySingleSignOn(login, secret.value);
    assertNotNull(conn2.login());
    conn2.logout();
    assertNull(conn2.login());

    // login repetido
    failed = false;
    try {
      manager.setRequester(conn);
      login = conn.startSingleSignOn(secret);
      manager.setRequester(conn2);
      conn2.loginBySingleSignOn(login, secret.value);
      assertNotNull(conn2.login());
      conn2.loginBySingleSignOn(login, secret.value);
    }
    catch (AlreadyLoggedIn e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exce��o deveria ser AlreadyLoggedInException. Exce��o recebida: "
        + e);
    }
    assertTrue("O login com entidade j� autenticada foi bem-sucedido.", failed);
    manager.setRequester(conn2);
    conn2.logout();
    manager.setRequester(conn);
    conn.logout();
    manager.setRequester(null);
  }

  @Test
  public void logoutTest() throws Exception {
    Connection conn = manager.createConnection(host, port);
    assertFalse(conn.logout());
    conn.loginByPassword(entity, password.getBytes());
    assertNotNull(conn.login());
    assertTrue(conn.logout());
    assertNull(conn.login());
    boolean failed = false;
    try {
      conn.offers().findServices(new ServiceProperty[0]);
    }
    catch (NO_PERMISSION e) {
      failed = true;
      if (e.minor != NoLoginCode.value) {
        fail("A exce��o � NO_PERMISSION mas o minor code n�o � NoLoginCode. Minor code recebido: "
          + e.minor);
      }
    }
    catch (Exception e) {
      fail("A exce��o deveria ser NO_PERMISSION. Exce��o recebida: " + e);
    }
    assertTrue("Uma busca sem login foi bem-sucedida.", failed);
  }

  @Test
  public void onInvalidLoginCallbackTest() throws Exception {
    Connection conn = manager.createConnection(host, port);
    assertNull(conn.onInvalidLoginCallback());
    InvalidLoginCallback callback = new InvalidLoginCallbackMock();
    conn.onInvalidLoginCallback(callback);
    assertEquals(callback, conn.onInvalidLoginCallback());
  }

  @Test
  public void callerChainTest() throws Exception {
    Connection conn = manager.createConnection(host, port);
    assertNull(conn.getCallerChain());
    //TODO: adicionar testes para caso exista uma callerchain ou os testes de interoperabilidade ja cobrem isso de forma suficiente?
  }

  @Test
  public void JoinChainTest() {
    Connection conn = manager.createConnection(host, port);
    assertNull(conn.getJoinedChain());
    // adiciona a chain da getCallerChain
    conn.joinChain(null);
    assertNull(conn.getJoinedChain());

    //TODO testar caso em que a chain da getCallerChain n�o � vazia
    //TODO comparar assinatura do callerchainimpl com a implementa��o CSHARP

    conn.joinChain(new CallerChainImpl("mock", new LoginInfo[] { new LoginInfo(
      "a", "b") }, null));

    CallerChain callerChain = conn.getJoinedChain();
    assertNotNull(callerChain);
    assertEquals("mock", callerChain.busid());
    assertEquals("a", callerChain.callers()[0].id);
    assertEquals("b", callerChain.callers()[0].entity);
    conn.exitChain();
  }

  @Test
  public void ExitChainTest() {
    Connection conn = manager.createConnection(host, port);
    assertNull(conn.getJoinedChain());
    conn.exitChain();
    assertNull(conn.getJoinedChain());
    conn.joinChain(new CallerChainImpl("mock", new LoginInfo[] { new LoginInfo(
      "a", "b") }, null));
    conn.exitChain();
    assertNull(conn.getJoinedChain());
  }
}
