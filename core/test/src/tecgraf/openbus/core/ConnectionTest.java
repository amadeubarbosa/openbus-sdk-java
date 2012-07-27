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
import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidBusAddress;
import tecgraf.openbus.exception.InvalidPrivateKey;
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
  public void offerRegistryTest() throws InvalidBusAddress {
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
    String busid = conn.busid();
    assertNotNull(conn.busid());
    conn.loginByPassword(entity, entity.getBytes());
    assertEquals(conn.busid(), busid);
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
      fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
    }
    assertTrue("O login com entidade vazia foi bem-sucedido.", failed);

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
      fail("A exceção deveria ser MissingCertificate. Exceção recebida: " + e);
    }
    assertTrue(
      "O login de entidade sem certificado cadastrado foi bem-sucedido.",
      failed);

    // chave privada corrompida
    failed = false;
    try {
      conn.loginByCertificate(serverEntity, new byte[0]);
    }
    catch (InvalidPrivateKey e) {
      failed = true;
    }
    catch (Exception e) {
      fail("A exceção deveria ser CorruptedPrivateKeyException. Exceção recebida: "
        + e);
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
  public void singleSignOnTest() throws Exception {
    Connection conn = manager.createConnection(host, port);
    Connection conn2 = manager.createConnection(host, port);
    conn.loginByPassword(entity, password.getBytes());

    // segredo errado
    boolean failed = false;
    OctetSeqHolder secret = new OctetSeqHolder();
    LoginProcess login;

    try {
      manager.setRequester(conn);
      login = conn.startSharedAuth(secret);
      manager.setRequester(null);
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
    manager.setRequester(conn);
    login = conn.startSharedAuth(secret);
    manager.setRequester(null);
    conn2.loginBySharedAuth(login, secret.value);
    assertNotNull(conn2.login());
    conn2.logout();
    assertNull(conn2.login());
    manager.setRequester(null);

    // login repetido
    failed = false;
    try {
      manager.setRequester(conn);
      login = conn.startSharedAuth(secret);
      manager.setRequester(null);
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
    manager.setRequester(null);
  }

  @Test
  public void logoutTest() throws Exception {
    Connection conn = manager.createConnection(host, port);
    assertFalse(conn.logout());
    conn.loginByPassword(entity, password.getBytes());
    String busId = conn.busid();
    manager.setDispatcher(conn);
    assertEquals(manager.getDispatcher(busId), conn);
    assertTrue(conn.logout());
    assertEquals(manager.getDispatcher(busId), conn);
    assertNotNull(conn.busid());
    assertNull(conn.login());
    boolean failed = false;
    try {
      manager.setRequester(conn);
      conn.offers().findServices(new ServiceProperty[0]);
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
  public void JoinChainTest() throws InvalidBusAddress {
    Connection conn = manager.createConnection(host, port);
    assertNull(conn.getJoinedChain());
    // adiciona a chain da getCallerChain
    conn.joinChain(null);
    assertNull(conn.getJoinedChain());

    //TODO testar caso em que a chain da getCallerChain não é vazia
    //TODO comparar assinatura do callerchainimpl com a implementação CSHARP

    conn.joinChain(new CallerChainImpl("mock", new LoginInfo("a", "b"),
      new LoginInfo[0], new SignedCallChain(new byte[256], new byte[0])));

    CallerChain callerChain = conn.getJoinedChain();
    assertNotNull(callerChain);
    assertEquals("mock", callerChain.busid());
    assertEquals("a", callerChain.caller().id);
    assertEquals("b", callerChain.caller().entity);
    conn.exitChain();
  }

  @Test
  public void ExitChainTest() throws InvalidBusAddress {
    Connection conn = manager.createConnection(host, port);
    assertNull(conn.getJoinedChain());
    conn.exitChain();
    assertNull(conn.getJoinedChain());
    conn.joinChain(new CallerChainImpl("mock", new LoginInfo("a", "b"),
      new LoginInfo[0], new SignedCallChain(new byte[256], new byte[0])));
    conn.exitChain();
    assertNull(conn.getJoinedChain());
  }
}
