package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.exception.SCSException;
import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_0.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_0.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidEncodedStream;
import tecgraf.openbus.exception.NotLoggedIn;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.util.Utils;
import test.CallerChainInspector;
import test.CallerChainInspectorHelper;

public final class OpenBusContextTest {
  private static String hostName;
  private static int hostPort;
  private static String entity;
  private static String password;
  private static String serverEntity;
  private static RSAPrivateKey privateKey;

  private static ORB orb;
  private static OpenBusContext context;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Properties properties = Utils.readPropertyFile("/test.properties");
    hostName = properties.getProperty("bus.host.name");
    hostPort = Integer.valueOf(properties.getProperty("bus.host.port"));
    entity = properties.getProperty("user.entity.name");
    password = properties.getProperty("user.password");
    serverEntity = properties.getProperty("system.entity.name");
    String privateKeyFile = properties.getProperty("system.private.key");
    privateKey = Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    orb = ORBInitializer.initORB();
    context = (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
  }

  @Before
  public void afterEachTest() {
    context.setCurrentConnection(null);
    context.setDefaultConnection(null);
    context.onCallDispatch(null);
    context.exitChain();
  }

  @Test
  public void ORBTest() {
    assertNotNull(context.orb());
  }

  @Test
  public void TwoORBsDefaultConnectionTest() throws InvalidName {
    Connection conn = context.createConnection(hostName, hostPort);
    assertNull(context.getDefaultConnection());
    context.setDefaultConnection(conn);
    assertNotNull(context.getDefaultConnection());
    assertEquals(conn, context.getDefaultConnection());

    ORB orb2 = ORBInitializer.initORB();
    assertNotSame(orb, orb2);

    OpenBusContext context2 =
      (OpenBusContext) orb2.resolve_initial_references("OpenBusContext");
    Connection conn2 = context2.createConnection(hostName, hostPort);
    assertNull(context2.getDefaultConnection());
    try {
      context2.setDefaultConnection(conn2);
      assertNotNull(context2.getDefaultConnection());
      assertEquals(conn2, context2.getDefaultConnection());
    }
    finally {
      context2.setDefaultConnection(null);
    }
  }

  @Test
  public void TwoORBsJoinChainTest() throws InvalidTypeForEncoding,
    UnknownEncoding, InvalidName, AccessDenied, AlreadyLoggedIn, ServiceFailure {
    assertNull(context.getJoinedChain());
    String busid = "mock";
    String target = "target";
    LoginInfo caller = new LoginInfo("a", "b");
    LoginInfo[] originators = new LoginInfo[0];
    context.joinChain(buildFakeCallChain(busid, target, caller, originators));

    ORB orb2 = ORBInitializer.initORB();
    assertNotSame(orb, orb2);

    OpenBusContext context2 =
      (OpenBusContext) orb2.resolve_initial_references("OpenBusContext");
    assertNull(context2.getJoinedChain());
    context.exitChain();
  }

  @Test
  public void createConnectionIllegalArgumentTest() {
    // cria conexão válida
    Connection valid = context.createConnection(hostName, hostPort);
    assertNotNull(valid);
    // tenta criar conexão com hosts inválidos
    Connection invalid = null;
    try {
      invalid = context.createConnection("", hostPort);
    }
    catch (IllegalArgumentException e) {
    }
    finally {
      assertNull(invalid);
    }
    try {
      invalid = context.createConnection(hostName, -1);
    }
    catch (IllegalArgumentException e) {
    }
    finally {
      assertNull(invalid);
    }
  }

  @Test
  public void createConnectionUknownHostTest() {
    // tenta criar conexão com hosts desconhecido
    Connection invalid = context.createConnection("unknowHost", hostPort);
    assertNotNull(invalid);
    // nenhuma chamada remota deve ser realizada e a conexão deve ser criada
  }

  @Test
  public void defaultConnectionTest() throws NotLoggedIn, AccessDenied,
    AlreadyLoggedIn, ServiceFailure {
    context.setDefaultConnection(null);
    final Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByPassword(entity, password.getBytes());
    assertNull(context.getDefaultConnection());
    context.setCurrentConnection(conn);
    assertNull(context.getDefaultConnection());
    context.setCurrentConnection(null);
    context.setDefaultConnection(conn);
    assertEquals(context.getDefaultConnection(), conn);
    context.onCallDispatch(new CallDispatchCallback() {
      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        return conn;
      }
    });
    assertEquals(context.getDefaultConnection(), conn);
    context.onCallDispatch(null);
    assertTrue(conn.logout());
    assertEquals(context.getDefaultConnection(), conn);
    context.setDefaultConnection(null);
  }

  @Test
  public void requesterTest() throws AccessDenied, AlreadyLoggedIn,
    ServiceFailure, NotLoggedIn {
    final Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByPassword(entity, password.getBytes());
    assertNull(context.getCurrentConnection());
    context.setDefaultConnection(conn);
    context.onCallDispatch(new CallDispatchCallback() {
      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        return conn;
      }
    });
    assertNotNull(context.getCurrentConnection());
    assertEquals(context.getCurrentConnection(), context.getDefaultConnection());
    context.setCurrentConnection(conn);
    assertEquals(context.getCurrentConnection(), conn);
    context.setDefaultConnection(null);
    context.onCallDispatch(null);
    assertTrue(conn.logout());
    assertEquals(context.getCurrentConnection(), conn);
    context.setCurrentConnection(null);

    // tentativa de chamada sem conexão request setada
    conn.loginByPassword(entity, password.getBytes());
    assertNull(context.getCurrentConnection());
    boolean failed = false;
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("a", "b") };
    try {
      context.getOfferRegistry().findServices(props);
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
    // tentativa com conexão de request setada
    context.setCurrentConnection(conn);
    try {
      context.getOfferRegistry().findServices(props);
    }
    catch (Exception e) {
      fail("A chamada com conexão setada deveria ser bem-sucedida. Exceção recebida: "
        + e);
    }
  }

  @Test
  public void callerChainTest() throws Exception {
    Connection conn = context.createConnection(hostName, hostPort);
    assertNull(context.getCallerChain());
    //atualmente os testes de interoperabilidade ja cobrem esses testes
  }

  @Test
  public void getCallerChainInDispatchTest() throws Exception {
    Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByPassword(entity, password.getBytes());
    context.setDefaultConnection(conn);
    ComponentContext component = Utils.buildTestCallerChainComponent(context);
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "OpenBusContextTest") };
    ServiceOffer offer =
      context.getOfferRegistry().registerService(component.getIComponent(),
        props);
    ServiceOfferDesc[] services =
      context.getOfferRegistry().findServices(props);
    assertEquals(1, services.length);
    assertNotNull(services[0]);
    offer.remove();
    context.setDefaultConnection(null);
    conn.logout();
  }

  @Test
  public void getConnectionInDispatchTest() throws Exception {
    Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByPassword(entity, password.getBytes());
    context.setDefaultConnection(conn);
    ComponentContext component = Utils.buildTestConnectionComponent(context);
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "OpenBusContextTest") };
    ServiceOffer offer =
      context.getOfferRegistry().registerService(component.getIComponent(),
        props);
    ServiceOfferDesc[] services =
      context.getOfferRegistry().findServices(props);
    assertEquals(1, services.length);
    assertNotNull(services[0]);
    offer.remove();
    context.setDefaultConnection(null);
    conn.logout();
  }

  @Test
  public void getConnectionInDispatch2Test() throws Exception {
    final Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByPassword(entity, password.getBytes());
    context.setCurrentConnection(conn);
    context.onCallDispatch(new CallDispatchCallback() {
      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        return conn;
      }
    });
    ComponentContext component = Utils.buildTestConnectionComponent(context);
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "OpenBusContextTest") };
    ServiceOffer offer =
      context.getOfferRegistry().registerService(component.getIComponent(),
        props);
    ServiceOfferDesc[] services =
      context.getOfferRegistry().findServices(props);
    assertEquals(1, services.length);
    assertNotNull(services[0]);
    offer.remove();
    context.setCurrentConnection(null);
    conn.logout();
  }

  @Test
  public void joinChainTest() throws InvalidTypeForEncoding, UnknownEncoding,
    InvalidName {
    Connection conn = context.createConnection(hostName, hostPort);
    assertNull(context.getJoinedChain());
    // adiciona a chain da getCallerChain
    context.joinChain(null);
    assertNull(context.getJoinedChain());

    String busid = "mock";
    String target = "target";
    LoginInfo caller = new LoginInfo("a", "b");
    LoginInfo[] originators = new LoginInfo[0];

    context.joinChain(buildFakeCallChain(busid, target, caller, originators));

    CallerChain callerChain = context.getJoinedChain();
    assertNotNull(callerChain);
    assertEquals(busid, callerChain.busid());
    assertEquals(target, callerChain.target());
    assertEquals("a", callerChain.caller().id);
    assertEquals("b", callerChain.caller().entity);
    context.exitChain();
  }

  @Test
  public void exitChainTest() throws InvalidTypeForEncoding, UnknownEncoding,
    InvalidName {
    Connection conn = context.createConnection(hostName, hostPort);
    assertNull(context.getJoinedChain());
    context.exitChain();
    assertNull(context.getJoinedChain());
    context.joinChain(buildFakeCallChain("mock", "target", new LoginInfo("a",
      "b"), new LoginInfo[0]));
    assertNotNull(context.getJoinedChain());
    context.exitChain();
    assertNull(context.getJoinedChain());
  }

  @Test
  public void makeChainForTest() throws AccessDenied, AlreadyLoggedIn,
    ServiceFailure, InvalidLogins {
    Connection conn1 = context.createConnection(hostName, hostPort);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes());
    Connection conn2 = context.createConnection(hostName, hostPort);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes());

    context.setCurrentConnection(conn1);
    CallerChain chain1to2 = context.makeChainFor(conn2.login().id);
    assertEquals(actor2, chain1to2.target());
    assertEquals(actor1, chain1to2.caller().entity);
    assertEquals(conn1.login().id, chain1to2.caller().id);

    conn1.logout();
    conn2.logout();
  }

  @Test
  public void makeChainForJoinedTest() throws AccessDenied, AlreadyLoggedIn,
    ServiceFailure, InvalidLogins {
    Connection conn1 = context.createConnection(hostName, hostPort);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes());
    Connection conn2 = context.createConnection(hostName, hostPort);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes());
    Connection conn3 = context.createConnection(hostName, hostPort);
    String actor3 = "actor-3";
    conn3.loginByPassword(actor3, actor3.getBytes());

    context.setCurrentConnection(conn1);
    CallerChain chain1to2 = context.makeChainFor(conn2.login().id);
    assertEquals(actor2, chain1to2.target());
    assertEquals(actor1, chain1to2.caller().entity);
    assertEquals(conn1.login().id, chain1to2.caller().id);

    context.setCurrentConnection(conn2);
    context.joinChain(chain1to2);
    CallerChain chain1_2to3 = context.makeChainFor(conn3.login().id);
    assertEquals(actor3, chain1_2to3.target());
    assertEquals(actor2, chain1_2to3.caller().entity);
    assertEquals(conn2.login().id, chain1_2to3.caller().id);
    LoginInfo[] originators = chain1_2to3.originators();
    assertTrue(originators.length > 0);
    LoginInfo info1 = originators[0];
    assertEquals(actor1, info1.entity);
    assertEquals(conn1.login().id, info1.id);

    context.exitChain();
    conn1.logout();
    conn2.logout();
    conn3.logout();
  }

  @Test(expected = NO_PERMISSION.class)
  public void makeChainForJoinedLegacyTest() throws AccessDenied,
    AlreadyLoggedIn, ServiceFailure, InvalidLogins, InvalidTypeForEncoding,
    UnknownEncoding, InvalidName, MissingCertificate {
    Connection conn1 = context.createConnection(hostName, hostPort);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes());
    Connection conn2 = context.createConnection(hostName, hostPort);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes());
    Connection conn3 = context.createConnection(hostName, hostPort);
    String actor3 = "actor-3";
    conn3.loginByPassword(actor3, actor3.getBytes());

    try {
      String delegate = "";
      CallerChain legacyChain =
        buildFakeLegacyCallChain(conn1.busid(), conn1.login(), actor2, delegate);
      context.setCurrentConnection(conn2);
      context.joinChain(legacyChain);
      CallerChain joined2to3 = context.makeChainFor(conn3.login().id);
    }
    finally {
      context.exitChain();
      conn1.logout();
      conn2.logout();
      conn3.logout();
    }
  }

  @Test
  public void makeChainForJoinedLegacyWithDelegateTest() throws AccessDenied,
    AlreadyLoggedIn, ServiceFailure, InvalidLogins, InvalidTypeForEncoding,
    UnknownEncoding, InvalidName, MissingCertificate {
    Connection conn1 = context.createConnection(hostName, hostPort);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes());
    Connection conn2 = context.createConnection(hostName, hostPort);
    conn2.loginByCertificate(serverEntity, privateKey);
    Connection conn3 = context.createConnection(hostName, hostPort);
    String actor3 = "actor-3";
    conn3.loginByPassword(actor3, actor3.getBytes());

    try {
      String delegate = "user1";
      CallerChain legacyChain =
        buildFakeLegacyCallChain(conn1.busid(), conn1.login(), serverEntity,
          delegate);
      context.setCurrentConnection(conn2);
      context.joinChain(legacyChain);
      CallerChain joined2to3 = context.makeChainFor(conn3.login().id);
      assertEquals(conn2.busid(), joined2to3.busid());
      assertEquals(actor3, joined2to3.target());
      assertEquals(serverEntity, joined2to3.caller().entity);
      assertEquals(conn2.login().id, joined2to3.caller().id);
      assertTrue(joined2to3.originators().length > 0);
      assertEquals(actor1, joined2to3.originators()[0].entity);
      assertEquals("<unknown>", joined2to3.originators()[0].id);
    }
    finally {
      context.exitChain();
      conn1.logout();
      conn2.logout();
      conn3.logout();
    }
  }

  @Test
  public void makeChainForInvalidLoginTest() throws AccessDenied,
    AlreadyLoggedIn, ServiceFailure, InvalidLogins {
    Connection conn1 = context.createConnection(hostName, hostPort);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes());

    context.setCurrentConnection(conn1);
    boolean failed = false;
    String invalidLogin = "invalid-login-id";
    try {
      context.makeChainFor(invalidLogin);
    }
    catch (InvalidLogins e) {
      failed = true;
      String[] loginIds = e.loginIds;
      assertEquals(loginIds.length, 1);
      assertEquals(loginIds[0], invalidLogin);
    }
    assertTrue(failed);

    Connection conn2 = context.createConnection(hostName, hostPort);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes());
    String oldLogin2 = conn2.login().id;
    conn2.logout();

    failed = false;
    try {
      context.makeChainFor(oldLogin2);
    }
    catch (InvalidLogins e) {
      failed = true;
      String[] loginIds = e.loginIds;
      assertEquals(loginIds.length, 1);
      assertEquals(loginIds[0], oldLogin2);
    }
    assertTrue(failed);
    conn1.logout();
  }

  @Test
  public void simulateCallTest() throws AccessDenied, AlreadyLoggedIn,
    ServiceFailure, InvalidLogins, MissingCertificate, InvalidService,
    UnauthorizedFacets, InvalidProperties, AdapterInactive, InvalidName,
    SCSException {
    Connection conn1 = context.createConnection(hostName, hostPort);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes());
    String login1 = conn1.login().id;
    Connection conn2 = context.createConnection(hostName, hostPort);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes());
    String login2 = conn2.login().id;
    Connection conn3 = context.createConnection(hostName, hostPort);
    String actor3 = "actor-3";
    conn3.loginByPassword(actor3, actor3.getBytes());
    String login3 = conn3.login().id;

    final Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByCertificate(serverEntity, privateKey);
    context.onCallDispatch(new CallDispatchCallback() {

      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        return conn;
      }
    });

    context.setCurrentConnection(conn1);
    CallerChain chain1For2 = context.makeChainFor(conn2.login().id);
    CallerChain chain1For3 = context.makeChainFor(conn3.login().id);
    context.setCurrentConnection(conn2);
    context.joinChain(chain1For2);
    CallerChain chain1_2For3 = context.makeChainFor(conn3.login().id);
    context.setCurrentConnection(conn3);

    context.setCurrentConnection(conn);
    context.exitChain();
    ComponentContext component =
      Utils.buildTestCallerChainInspectorComponent(context);
    ServiceOffer offer =
      context.getOfferRegistry().registerService(component.getIComponent(),
        new ServiceProperty[0]);

    context.setCurrentConnection(conn3);
    Object object =
      offer.service_ref().getFacet(CallerChainInspectorHelper.id());
    CallerChainInspector inspector = CallerChainInspectorHelper.narrow(object);

    context.joinChain(chain1For3);
    String[] callers1_3 = new String[] { actor1, actor3 };
    String[] logins1_3 = new String[] { login1, login3 };

    String[] callers = inspector.listCallers();
    assertEquals(callers1_3.length, callers.length);
    for (int i = 0; i < callers.length; i++) {
      assertEquals(callers1_3[i], callers[i]);
    }

    String[] logins = inspector.listCallerLogins();
    assertEquals(logins1_3.length, logins.length);
    for (int i = 0; i < logins.length; i++) {
      assertEquals(logins1_3[i], logins[i]);
    }

    context.exitChain();
    conn1.logout();
    conn2.logout();

    context.joinChain(chain1_2For3);
    String[] callers1_2_3 = new String[] { actor1, actor2, actor3 };
    String[] logins1_2_3 = new String[] { login1, login2, login3 };

    callers = inspector.listCallers();
    assertEquals(callers1_2_3.length, callers.length);
    for (int i = 0; i < callers.length; i++) {
      assertEquals(callers1_2_3[i], callers[i]);
    }

    logins = inspector.listCallerLogins();
    assertEquals(logins1_2_3.length, logins.length);
    for (int i = 0; i < logins.length; i++) {
      assertEquals(logins1_2_3[i], logins[i]);
    }

    conn3.logout();
    conn.logout();
  }

  @Test
  public void encodeAndDecodeChain() throws AccessDenied, AlreadyLoggedIn,
    ServiceFailure, InvalidLogins, InvalidEncodedStream {
    Connection conn1 = context.createConnection(hostName, hostPort);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes());
    String login1 = conn1.login().id;
    Connection conn2 = context.createConnection(hostName, hostPort);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes());
    String login2 = conn2.login().id;
    Connection conn3 = context.createConnection(hostName, hostPort);
    String actor3 = "actor-3";
    conn3.loginByPassword(actor3, actor3.getBytes());
    String login3 = conn3.login().id;

    context.setCurrentConnection(conn1);
    CallerChain chain1For2 = context.makeChainFor(login2);

    byte[] encodeChain = context.encodeChain(chain1For2);
    CallerChain decodedChain = context.decodeChain(encodeChain);
    assertEquals(conn1.busid(), decodedChain.busid());
    assertEquals(conn2.busid(), decodedChain.busid());
    assertEquals(actor2, decodedChain.target());
    assertEquals(actor1, decodedChain.caller().entity);
    assertEquals(login1, decodedChain.caller().id);

    context.setCurrentConnection(conn2);
    context.joinChain(decodedChain);
    CallerChain chain1_2For3 = context.makeChainFor(login3);

    encodeChain = context.encodeChain(chain1_2For3);
    decodedChain = context.decodeChain(encodeChain);

    assertEquals(conn1.busid(), decodedChain.busid());
    assertEquals(conn2.busid(), decodedChain.busid());
    assertEquals(conn3.busid(), decodedChain.busid());
    assertEquals(actor3, decodedChain.target());
    assertEquals(actor2, decodedChain.caller().entity);
    assertEquals(login2, decodedChain.caller().id);
    LoginInfo[] originators = decodedChain.originators();
    assertTrue(originators.length > 0);
    LoginInfo info1 = originators[0];
    assertEquals(actor1, info1.entity);
    assertEquals(login1, info1.id);

    context.exitChain();
    conn1.logout();
    conn2.logout();
    conn3.logout();
  }

  @Test
  public void encodeAndDecodeLegacyChain() throws Exception {
    Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByPassword(entity, password.getBytes());
    LoginInfo login = conn.login();
    String busid = conn.busid();
    conn.logout();
    String delegate = "";
    String target = "anyone";
    CallerChain legacyChain =
      buildFakeLegacyCallChain(busid, login, target, delegate);
    byte[] encodeChain = context.encodeChain(legacyChain);
    CallerChain decodedChain = context.decodeChain(encodeChain);
    assertEquals(busid, decodedChain.busid());
    assertEquals(target, decodedChain.target());
    assertEquals(login.entity, decodedChain.caller().entity);
    assertEquals(login.id, decodedChain.caller().id);
    assertEquals(0, decodedChain.originators().length);

    delegate = "user-orgin";
    legacyChain = buildFakeLegacyCallChain(busid, login, target, delegate);
    byte[] encodedChainDelegated = context.encodeChain(legacyChain);
    CallerChain delegatedChain = context.decodeChain(encodedChainDelegated);
    assertEquals(busid, delegatedChain.busid());
    assertEquals(target, delegatedChain.target());
    assertEquals(login.entity, delegatedChain.caller().entity);
    assertEquals(login.id, delegatedChain.caller().id);
    assertEquals(1, delegatedChain.originators().length);
    assertEquals(delegate, delegatedChain.originators()[0].entity);
    assertEquals("<unknown>", delegatedChain.originators()[0].id);
  }

  @Test
  public void encodeAndDecodeSharedAuth() throws Exception {
    Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByPassword(entity, password.getBytes());
    try {
      context.setCurrentConnection(conn);
      SharedAuthSecret secret = conn.startSharedAuth();
      byte[] data = context.encodeSharedAuth(secret);
      Connection conn2 = context.createConnection(hostName, hostPort);
      SharedAuthSecret secret2 = context.decodeSharedAuth(data);
      conn2.loginBySharedAuth(secret2);
      assertNotNull(conn2.login());
      assertFalse(conn.login().id.equals(conn2.login().id));
      assertEquals(conn.login().entity, conn2.login().entity);
    }
    finally {
      context.setCurrentConnection(null);
    }
  }

  @Test(expected = InvalidEncodedStream.class)
  public void decodeSharedAuthAsChain() throws Exception {
    Connection conn = context.createConnection(hostName, hostPort);
    conn.loginByPassword(entity, password.getBytes());
    SharedAuthSecret secret = null;
    try {
      context.setCurrentConnection(conn);
      secret = conn.startSharedAuth();
      byte[] data = context.encodeSharedAuth(secret);
      context.decodeChain(data);
    }
    finally {
      secret.cancel();
      conn.logout();
      context.setCurrentConnection(null);
    }
  }

  @Test(expected = InvalidEncodedStream.class)
  public void decodeChainAsSharedAuth() throws Exception {
    Connection conn1 = context.createConnection(hostName, hostPort);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes());
    Connection conn2 = context.createConnection(hostName, hostPort);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes());
    String login2 = conn2.login().id;
    try {
      context.setCurrentConnection(conn1);
      CallerChain chain1For2 = context.makeChainFor(login2);
      byte[] data = context.encodeChain(chain1For2);
      context.decodeSharedAuth(data);
    }
    finally {
      context.setCurrentConnection(null);
      conn1.logout();
      conn2.logout();
    }
  }

  private Codec getCodec(ORB orb) throws UnknownEncoding, InvalidName {
    org.omg.CORBA.Object obj;
    obj = orb.resolve_initial_references("CodecFactory");
    CodecFactory codecFactory = CodecFactoryHelper.narrow(obj);
    byte major = 1;
    byte minor = 2;
    Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, major, minor);
    return codecFactory.create_codec(encoding);
  }

  private CallerChain buildFakeCallChain(String busid, String target,
    LoginInfo caller, LoginInfo[] originators) throws InvalidTypeForEncoding,
    UnknownEncoding, InvalidName {
    CallChain callChain = new CallChain(target, originators, caller);
    Any anyCallChain = orb.create_any();
    CallChainHelper.insert(anyCallChain, callChain);
    byte[] encodedCallChain = getCodec(orb).encode_value(anyCallChain);
    return new CallerChainImpl(busid, target, caller, originators,
      new SignedCallChain(new byte[256], encodedCallChain));
  }

  private CallerChain buildFakeLegacyCallChain(String busid, LoginInfo caller,
    String target, String delegate) throws InvalidTypeForEncoding,
    UnknownEncoding, InvalidName {
    LoginInfo[] originators;
    if (delegate != null) {
      originators = new LoginInfo[1];
      originators[0] = new LoginInfo("<unknown>", delegate);
    }
    else {
      originators = new LoginInfo[0];
    }
    CallChain callChain = new CallChain(target, originators, caller);
    SignedCallChain signedChain =
      LegacySupport.buildLegacySignedChain(callChain, orb, getCodec(orb));
    return new CallerChainImpl(busid, callChain.target, callChain.caller,
      callChain.originators, signedChain);
  }

}
