package tecgraf.openbus.core;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.Object;
import org.omg.CORBA.TRANSIENT;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import scs.core.ComponentContext;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.v2_1.BusObjectKey;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidToken;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidEncodedStream;
import tecgraf.openbus.exception.InvalidPropertyValue;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.util.Builder;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.Utils;
import test.CallerChainInspector;
import test.CallerChainInspectorHelper;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public final class OpenBusContextTest {
  private static ORB orb;
  private static OpenBusContextImpl context;
  private static Object busref;
  private static String host;
  private static int port;
  private static String entity;
  private static byte[] password;
  private static String domain;
  private static String admin;
  private static byte[] adminPsw;
  private static String system;
  private static RSAPrivateKey systemKey;
  private static String systemKeyPath;
  private static X509Certificate busCert;
  private static String busCertPath;
  private static int threadNumber;
  private static long interval;
  private static TimeUnit intervalUnit;
  private static Properties orbprops;
  private static long sleepTime;
  private final TimeUnit sleepTimeUnit = TimeUnit.MILLISECONDS;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Configs configs = Configs.readConfigsFile();
    Utils.setLibLogLevel(configs.log);
    host = configs.bushost;
    port = configs.busport;
    entity = configs.user;
    password = configs.password;
    domain = configs.domain;
    admin = configs.admin;
    adminPsw = configs.admpsw;
    system = configs.system;
    systemKeyPath = configs.syskey;
    systemKey = crypto.readKeyFromFile(systemKeyPath);
    busCertPath = configs.buscert;
    busCert = crypto.readX509Certificate(busCertPath);
    threadNumber = configs.threadNum;
    interval = configs.interval;
    intervalUnit = ConnectionImpl.convertUnitPropertyToTimeUnit(configs
      .intervalUnit);
    sleepTime = configs.sleepMsTime;
    orbprops = Utils.readPropertyFile(configs.orbprops);
    orb = ORBInitializer.initORB(null, orbprops);
    busref = orb.string_to_object(new String(Utils.readFile(configs.busref)));
    context = (OpenBusContextImpl) orb.resolve_initial_references
      ("OpenBusContext");
  }

  @Before
  public void afterEachTest() {
    context.currentConnection(null);
    context.defaultConnection(null);
    context.onCallDispatch(null);
    context.exitChain();
  }

  @Test
  public void ORBTest() {
    assertNotNull(context.ORB());
  }

  @Test
  public void TwoORBsDefaultConnectionTest() throws InvalidName {
    Connection conn = context.connectByReference(busref);
    assertNull(context.defaultConnection());
    context.defaultConnection(conn);
    assertNotNull(context.defaultConnection());
    assertSame(conn, context.defaultConnection());

    ORB orb2 = ORBInitializer.initORB(null, orbprops);
    assertNotSame(orb, orb2);

    OpenBusContext context2 =
      (OpenBusContext) orb2.resolve_initial_references("OpenBusContext");
    Connection conn2 = context2.connectByReference(busref);
    assertNull(context2.defaultConnection());
    try {
      context2.defaultConnection(conn2);
      assertNotNull(context2.defaultConnection());
      assertSame(conn2, context2.defaultConnection());
    }
    finally {
      context2.defaultConnection(null);
    }
  }

  @Test
  public void TwoORBsJoinChainTest() throws InvalidTypeForEncoding,
    UnknownEncoding, InvalidName, AccessDenied, AlreadyLoggedIn, ServiceFailure {
    assertNull(context.joinedChain());
    String busid = "mock";
    String target = "target";
    LoginInfo caller = new LoginInfo("a", "b");
    LoginInfo[] originators = new LoginInfo[0];
    context.joinChain(buildFakeCallChain(busid, target, caller, originators));

    ORB orb2 = ORBInitializer.initORB(null, orbprops);
    assertNotSame(orb, orb2);

    OpenBusContext context2 =
      (OpenBusContext) orb2.resolve_initial_references("OpenBusContext");
    assertNull(context2.joinedChain());
    context.exitChain();
  }

  @Test
  public void createConnectionIllegalArgumentTest() {
    // tenta criar conexão com hosts inválidos
    Connection invalid = null;
    try {
      invalid = context.connectByAddress("", port);
    }
    catch (IllegalArgumentException ignored) {
    }
    finally {
      assertNull(invalid);
    }
    try {
      invalid = context.connectByAddress(host, -1);
    }
    catch (IllegalArgumentException ignored) {
    }
    finally {
      assertNull(invalid);
    }
  }

  @Test
  public void createConnectionUknownHostTest() {
    // tenta criar conexão com hosts desconhecido
    Connection invalid = context.connectByAddress("unknowHost", port);
    assertNotNull(invalid);
    // nenhuma chamada remota deve ser realizada e a conexão deve ser criada
  }

  @Test(expected = IllegalArgumentException.class)
  public void connectByReferenceIllegalArgumentTest() {
    context.connectByReference(null);
  }

  @Test(expected = TRANSIENT.class)
  public void connectByReferenceUknownHostTest() throws Exception {
    String host = "unknowHost";
    String str =
      String.format("corbaloc::1.0@%s:%d/%s", host, port, BusObjectKey.value);
    org.omg.CORBA.Object obj = orb.string_to_object(str);
    Connection invalid = context.connectByReference(obj);
    assertNotNull(invalid);
    invalid.loginByPassword(entity, password, domain);
  }

  @Test
  public void connectByReferenceTest() throws Exception {
    final Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    assertNotNull(conn.login());
    assertTrue(conn.logout());
  }

  @Test
  public void accessKeyPropTest() throws Exception {
    Properties properties = new Properties();
    properties.put(OpenBusProperty.ACCESS_KEY.getKey(), systemKeyPath);
    Connection conn = context.connectByReference(busref, properties);
    assertNull(conn.login());
    conn.loginByPassword(entity, entity.getBytes(), domain);
    assertNotNull(conn.login());
    conn.logout();
    assertNull(conn.login());
  }

  @Test(expected = InvalidPropertyValue.class)
  public void invalidAccessKeyPropTest() throws Exception {
    Properties properties = new Properties();
    properties.put(OpenBusProperty.ACCESS_KEY.getKey(),
      "/invalid/path/to/access.key");
    context.connectByReference(busref, properties);
  }

  @Test
  public void busCertificatePropTest() throws Exception {
    Properties properties = new Properties();
    properties.put(OpenBusProperty.BUS_CERTIFICATE.getKey(), busCertPath);
    ConnectionImpl conn = (ConnectionImpl) context.connectByReference(busref,
      properties);
    assertNull(conn.login());
    conn.loginByPassword(entity, entity.getBytes(), domain);
    assertNotNull(conn.login());
    assertSame(conn.busPublicKey(), busCert.getPublicKey());
    conn.logout();
    assertNull(conn.login());
  }

  @Test
  public void threadNumberPropTest() throws Exception {
    Properties properties = new Properties();
    properties.put(OpenBusProperty.THREAD_NUMBER.getKey(), "" + threadNumber);
    Connection conn = context.connectByReference(busref, properties);
    assertNull(conn.login());
    conn.loginByPassword(entity, entity.getBytes(), domain);
    assertNotNull(conn.login());
    LoginRegistryImpl loginRegistry = (LoginRegistryImpl)conn.loginRegistry();
    OfferRegistryImpl offerRegistry = (OfferRegistryImpl)conn.offerRegistry();
    assertEquals(threadNumber, loginRegistry.pool().poolSize);
    assertEquals(threadNumber, offerRegistry.pool().poolSize);
    conn.logout();
    assertNull(conn.login());
  }

  @Test
  public void intervalPropTest() throws Exception {
    Properties properties = new Properties();
    properties.put(OpenBusProperty.TIME_INTERVAL.getKey(), "" + interval);
    properties.put(OpenBusProperty.TIME_UNIT.getKey(), "" + intervalUnit);
    Connection conn = context.connectByReference(busref, properties);
    assertNull(conn.login());
    conn.loginByPassword(entity, entity.getBytes(), domain);
    assertNotNull(conn.login());
    LoginRegistryImpl loginRegistry = (LoginRegistryImpl)conn.loginRegistry();
    OfferRegistryImpl offerRegistry = (OfferRegistryImpl)conn.offerRegistry();
    assertEquals(interval, loginRegistry.interval());
    assertEquals(interval, offerRegistry.interval());
    assertEquals(intervalUnit, loginRegistry.intervalUnit());
    assertEquals(intervalUnit, offerRegistry.intervalUnit());
    conn.logout();
    assertNull(conn.login());
  }

  @Test
  public void invalidHostPortLoginTest() throws Exception {
    Connection conn = context.connectByAddress("unknown-host", port);
    assertNull(conn.login());
    try {
      conn.loginByPassword(entity, entity.getBytes(), domain);
    }
    catch (TRANSIENT e) {
      // erro esperado
    }
    catch (Exception e) {
      fail("A exceção deveria ser TRANSIENT. Exceção recebida: " + e);
    }
    assertNull(conn.login());
    // chutando uma porta inválida
    conn = context.connectByAddress(host, port + 111);
    assertNull(conn.login());
    try {
      conn.loginByPassword(entity, entity.getBytes(), domain);
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
  public void defaultConnectionTest() throws Exception {
    context.defaultConnection(null);
    final Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    assertNull(context.defaultConnection());
    context.currentConnection(conn);
    assertNull(context.defaultConnection());
    context.currentConnection(null);
    context.defaultConnection(conn);
    assertSame(context.defaultConnection(), conn);
    context.onCallDispatch((context1, busid, loginId, object_id, operation)
      -> conn);
    assertSame(context.defaultConnection(), conn);
    context.onCallDispatch(null);
    assertTrue(conn.logout());
    assertSame(context.defaultConnection(), conn);
    context.defaultConnection(null);
  }

  @Test
  public void currentConnectionTest() throws WrongEncoding, AlreadyLoggedIn,
    ServiceFailure, UnknownDomain, TooManyAttempts, AccessDenied {

    final Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    assertNull(context.currentConnection());
    context.defaultConnection(conn);
    context.onCallDispatch((context1, busid, loginId, object_id, operation)
      -> conn);
    assertSame(context.currentConnection(), conn);
    context.currentConnection(conn);
    assertSame(context.currentConnection(), conn);
    context.defaultConnection(null);
    context.onCallDispatch(null);
    assertTrue(conn.logout());
    assertSame(context.currentConnection(), conn);
    Connection previous = context.currentConnection(null);
    assertSame(previous, conn);

    // tentativa de chamada sem current connection setado nem default connection
    conn.loginByPassword(entity, password, domain);
    assertNull(context.currentConnection());
    boolean failed = false;
    ServiceProperty[] props = {new ServiceProperty("a", "b")};
    try {
      context.getOfferRegistry().findServices(props);
    }
    catch (NO_PERMISSION e) {
      failed = true;
      if (e.minor != NoLoginCode.value) {
        fail("A exceção deveria ser NO_PERMISSION com minor code NoLoginCode." +
          " Minor code recebido: " + e.minor);
      }
    }
    catch (Exception e) {
      fail("A exceção deveria ser NO_PERMISSION com minor code NoLoginCode. " +
        "Exceção recebida: " + e);
    }
    assertTrue(failed);
    // tentativa com current connection setado
    previous = context.currentConnection(conn);
    assertNull(previous);
    try {
      context.getOfferRegistry().findServices(props);
    }
    catch (Exception e) {
      fail("A chamada com current connection setado deveria ser bem-sucedida." +
        " Exceção recebida: " + e);
    }
    finally {
      previous = context.currentConnection(null);
    }
    assertSame(previous, conn);
  }

  @Test
  public void requesterTest() throws Exception {
    final Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    assertNull(context.currentConnection());
    context.defaultConnection(conn);
    context.onCallDispatch((context1, busid, loginId, object_id, operation)
      -> conn);
    assertNotNull(context.currentConnection());
    assertSame(context.currentConnection(), context.defaultConnection());
    context.currentConnection(conn);
    assertSame(context.currentConnection(), conn);
    context.defaultConnection(null);
    context.onCallDispatch(null);
    assertTrue(conn.logout());
    assertSame(context.currentConnection(), conn);
    context.currentConnection(null);

    // tentativa de chamada sem conexão request setada
    conn.loginByPassword(entity, password, domain);
    assertNull(context.currentConnection());
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
    context.currentConnection(conn);
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
    Connection conn = context.connectByReference(busref);
    assertNull(context.callerChain());
    // atualmente os testes de interoperabilidade ja cobrem esses testes
  }

  @Test
  public void getCallerChainInDispatchTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    context.defaultConnection(conn);
    ComponentContext component = Builder.buildTestCallerChainComponent(context);
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
    context.defaultConnection(null);
    conn.logout();
  }

  @Test
  public void getConnectionInDispatchTest() throws Exception {
    Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    context.defaultConnection(conn);
    ComponentContext component = Builder.buildTestConnectionComponent(context);
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
    context.defaultConnection(null);
    conn.logout();
  }

  @Test
  public void getConnectionInDispatch2Test() throws Exception {
    final Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    context.currentConnection(conn);
    context.onCallDispatch((context1, busid, loginId, object_id, operation)
      -> conn);
    ComponentContext component = Builder.buildTestConnectionComponent(context);
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
    context.currentConnection(null);
    conn.logout();
  }

  @Test
  public void joinChainTest() throws InvalidTypeForEncoding, UnknownEncoding,
    InvalidName {
    Connection conn = context.connectByReference(busref);
    assertNull(context.joinedChain());
    // adiciona a chain da callerChain
    context.joinChain(null);
    assertNull(context.joinedChain());

    String busid = "mock";
    String target = "target";
    LoginInfo caller = new LoginInfo("a", "b");
    LoginInfo[] originators = new LoginInfo[0];

    context.joinChain(buildFakeCallChain(busid, target, caller, originators));

    CallerChain callerChain = context.joinedChain();
    assertNotNull(callerChain);
    assertEquals(busid, callerChain.busId());
    assertEquals(target, callerChain.target());
    assertEquals("a", callerChain.caller().id);
    assertEquals("b", callerChain.caller().entity);
    context.exitChain();
  }

  @Test
  public void exitChainTest() throws InvalidTypeForEncoding, UnknownEncoding,
    InvalidName {
    Connection conn = context.connectByReference(busref);
    assertNull(context.joinedChain());
    context.exitChain();
    assertNull(context.joinedChain());
    context.joinChain(buildFakeCallChain("mock", "target", new LoginInfo("a",
      "b"), new LoginInfo[0]));
    assertNotNull(context.joinedChain());
    context.exitChain();
    assertNull(context.joinedChain());
  }

  @Test
  public void importChainTest() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);
    String caller = "ExternalCaller";
    StringBuilder buffer = new StringBuilder();
    int origs = 2;
    for (int i = 1; i <= origs; i++) {
      buffer.append("ExternalOriginator").append(i).append(", ");
    }
    buffer.append(caller);
    String token =
      String.format("%s@%s:%s", actor1, conn1.login().id, buffer.toString());
    try {
      CallerChain imported = conn1.importChain(token.getBytes(), domain);
      String unknown = "<unknown>";
      assertEquals(conn1.busId(), imported.busId());
      assertEquals(actor1, imported.target());
      assertNotEquals(unknown, imported.caller().id);
      assertEquals(caller, imported.caller().entity);
      assertEquals(origs, imported.originators().length);
      for (LoginInfo info : imported.originators()) {
        assertNotEquals(unknown, info.id);
      }
    }
    finally {
      conn1.logout();
    }
  }

  @Test(expected = InvalidToken.class)
  public void importChainInvalidTokenTest() throws Exception {
    String token = "any";
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);
    try {
      conn1.importChain(token.getBytes(), domain);
    }
    finally {
      conn1.logout();
    }
  }

  @Test
  public void makeChainForTest() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);
    Connection conn2 = context.connectByReference(busref);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes(), domain);

    CallerChain chain1to2 = conn1.makeChainFor(conn2.login().entity);
    assertEquals(actor2, chain1to2.target());
    assertEquals(actor1, chain1to2.caller().entity);
    assertEquals(conn1.login().id, chain1to2.caller().id);

    conn1.logout();
    conn2.logout();
  }

  @Test
  public void makeChainForJoinedTest() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);
    Connection conn2 = context.connectByReference(busref);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes(), domain);
    Connection conn3 = context.connectByReference(busref);
    String actor3 = "actor-3";
    conn3.loginByPassword(actor3, actor3.getBytes(), domain);

    CallerChain chain1to2 = conn1.makeChainFor(conn2.login().entity);
    assertEquals(actor2, chain1to2.target());
    assertEquals(actor1, chain1to2.caller().entity);
    assertEquals(conn1.login().id, chain1to2.caller().id);

    context.joinChain(chain1to2);
    CallerChain chain1_2to3 = conn2.makeChainFor(conn3.login().entity);
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

  @Test
  public void makeChainForInvalidEntityTest() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);

    String invalidEntity = "invalid-one";
    CallerChain chain = conn1.makeChainFor(invalidEntity);
    assertEquals(invalidEntity, chain.target());
    conn1.logout();
  }

  @Test
  public void simulateCallTest() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);
    String login1 = conn1.login().id;
    Connection conn2 = context.connectByReference(busref);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes(), domain);
    String login2 = conn2.login().id;
    Connection conn3 = context.connectByReference(busref);
    String actor3 = "actor-3";
    conn3.loginByPassword(actor3, actor3.getBytes(), domain);
    String login3 = conn3.login().id;

    final Connection conn = context.connectByReference(busref);
    conn.loginByPrivateKey(system, systemKey);
    context.onCallDispatch((context1, busid, loginId, object_id, operation)
      -> conn);

    CallerChain chain1For2 = conn1.makeChainFor(conn2.login().entity);
    CallerChain chain1For3 = conn1.makeChainFor(conn3.login().entity);
    context.joinChain(chain1For2);
    CallerChain chain1_2For3 = conn2.makeChainFor(conn3.login().entity);

    context.currentConnection(conn);
    context.exitChain();
    ComponentContext component =
      Builder.buildTestCallerChainInspectorComponent(context);
    ServiceOffer offer =
      context.getOfferRegistry().registerService(component.getIComponent(),
        new ServiceProperty[0]);

    context.currentConnection(conn3);
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
  public void encodeAndDecodeChain() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);
    String login1 = conn1.login().id;
    Connection conn2 = context.connectByReference(busref);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes(), domain);
    String login2 = conn2.login().id;
    Connection conn3 = context.connectByReference(busref);
    String actor3 = "actor-3";
    conn3.loginByPassword(actor3, actor3.getBytes(), domain);

    CallerChain chain1For2 = conn1.makeChainFor(actor2);

    byte[] encodeChain = context.encodeChain(chain1For2);
    CallerChain decodedChain = context.decodeChain(encodeChain);
    assertEquals(conn1.busId(), decodedChain.busId());
    assertEquals(conn2.busId(), decodedChain.busId());
    assertEquals(actor2, decodedChain.target());
    assertEquals(actor1, decodedChain.caller().entity);
    assertEquals(login1, decodedChain.caller().id);

    context.joinChain(decodedChain);
    CallerChain chain1_2For3 = conn2.makeChainFor(actor3);

    encodeChain = context.encodeChain(chain1_2For3);
    decodedChain = context.decodeChain(encodeChain);

    assertEquals(conn1.busId(), decodedChain.busId());
    assertEquals(conn2.busId(), decodedChain.busId());
    assertEquals(conn3.busId(), decodedChain.busId());
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
  public void encodeAndDecodeChainCheckLegacy() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);
    Connection conn2 = context.connectByReference(busref);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes(), domain);

    CallerChain chain1For2 = conn1.makeChainFor(actor2);

    byte[] encodeChain = context.encodeChain(chain1For2);
    CallerChainImpl decodedChain =
      (CallerChainImpl) context.decodeChain(encodeChain);

    assertFalse(InterceptorImpl.NULL_SIGNED_CALL_CHAIN.equals(decodedChain
      .internal_chain().signedChain));
    assertFalse(InterceptorImpl.NULL_SIGNED_LEGACY_CALL_CHAIN
      .equals(decodedChain.internal_chain().signedLegacy));

    conn1.logout();
    conn2.logout();
  }

  @Test
  public void encodeAndDecodeSharedAuth() throws Exception {
    Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    try {
      context.currentConnection(conn);
      SharedAuthSecret secret = conn.startSharedAuth();
      byte[] data = context.encodeSharedAuth(secret);
      Connection conn2 = context.connectByReference(busref);
      SharedAuthSecret secret2 = context.decodeSharedAuth(data);
      conn2.loginByCallback(() -> new AuthArgs(secret2));
      assertNotNull(conn2.login());
      assertFalse(conn.login().id.equals(conn2.login().id));
      assertEquals(conn.login().entity, conn2.login().entity);
    }
    finally {
      context.currentConnection(null);
    }
  }

  @Test(expected = InvalidEncodedStream.class)
  public void decodeSharedAuthAsChain() throws Exception {
    Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    SharedAuthSecret secret = null;
    try {
      context.currentConnection(conn);
      secret = conn.startSharedAuth();
      byte[] data = context.encodeSharedAuth(secret);
      context.decodeChain(data);
    }
    finally {
      if (secret != null) {
        secret.cancel();
      }
      conn.logout();
      context.currentConnection(null);
    }
  }

  @Test
  public void encodeAndDecodeSharedAuthCheckLegacy() throws Exception {
    Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, password, domain);
    try {
      context.currentConnection(conn);
      SharedAuthSecret secret = conn.startSharedAuth();
      byte[] data = context.encodeSharedAuth(secret);
      SharedAuthSecretImpl secret2 =
        (SharedAuthSecretImpl) context.decodeSharedAuth(data);

      assertNotNull(secret2.attempt());
      assertNotNull(secret2.legacy());
      secret.cancel();
      conn.logout();
    }
    finally {
      context.currentConnection(null);
    }
  }

  @Test(expected = InvalidEncodedStream.class)
  public void decodeChainAsSharedAuth() throws Exception {
    Connection conn1 = context.connectByReference(busref);
    String actor1 = "actor-1";
    conn1.loginByPassword(actor1, actor1.getBytes(), domain);
    Connection conn2 = context.connectByReference(busref);
    String actor2 = "actor-2";
    conn2.loginByPassword(actor2, actor2.getBytes(), domain);
    String login2 = conn2.login().id;
    try {
      CallerChain chain1For2 = conn1.makeChainFor(login2);
      byte[] data = context.encodeChain(chain1For2);
      context.decodeSharedAuth(data);
    }
    finally {
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
    CallChain callChain = new CallChain(busid, target, originators, caller);
    Any anyCallChain = orb.create_any();
    CallChainHelper.insert(anyCallChain, callChain);
    byte[] encodedCallChain = getCodec(orb).encode_value(anyCallChain);
    return new CallerChainImpl(
      new CallChain(busid, target, originators, caller), new SignedData(
        new byte[256], encodedCallChain));
  }

}
