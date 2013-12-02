package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import scs.core.ComponentContext;
import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.NotLoggedIn;
import tecgraf.openbus.util.Utils;

public final class OpenBusContextTest {
  private static String _hostName;
  private static int _hostPort;
  private static String entity;
  private static String password;

  private static ORB orb;
  private static OpenBusContext _context;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Properties properties = Utils.readPropertyFile("/test.properties");
    _hostName = properties.getProperty("openbus.host.name");
    _hostPort = Integer.valueOf(properties.getProperty("openbus.host.port"));
    entity = properties.getProperty("entity.name");
    password = properties.getProperty("entity.password");
    orb = ORBInitializer.initORB();
    _context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

    Logger logger = Logger.getLogger("tecgraf.openbus");
    logger.setLevel(Level.FINE);
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(Level.FINE);
    logger.addHandler(handler);
  }

  @Before
  public void beforeEachTest() {
    _context.setCurrentConnection(null);
    _context.setDefaultConnection(null);
    _context.onCallDispatch(null);
  }

  @Test
  public void ORBTest() {
    assertNotNull(_context.orb());
  }

  @Test
  public void createConnectionTest() {
    // cria conexão válida
    Connection valid = _context.createConnection(_hostName, _hostPort);
    assertNotNull(valid);
    // tenta criar conexão com hosts inválidos
    Connection invalid = null;
    try {
      invalid = _context.createConnection("", _hostPort);
    }
    catch (Exception e) {
    }
    finally {
      assertNull(invalid);
    }
    try {
      invalid = _context.createConnection(_hostName, -1);
    }
    catch (Exception e) {
    }
    finally {
      assertNull(invalid);
    }
  }

  @Test
  public void defaultConnectionTest() throws NotLoggedIn, AccessDenied,
    AlreadyLoggedIn, ServiceFailure {
    _context.setDefaultConnection(null);
    final Connection conn = _context.createConnection(_hostName, _hostPort);
    conn.loginByPassword(entity, password.getBytes());
    assertNull(_context.getDefaultConnection());
    _context.setCurrentConnection(conn);
    assertNull(_context.getDefaultConnection());
    _context.setCurrentConnection(null);
    _context.setDefaultConnection(conn);
    assertEquals(_context.getDefaultConnection(), conn);
    _context.onCallDispatch(new CallDispatchCallback() {
      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        return conn;
      }
    });
    assertEquals(_context.getDefaultConnection(), conn);
    _context.onCallDispatch(null);
    assertTrue(conn.logout());
    assertEquals(_context.getDefaultConnection(), conn);
    _context.setDefaultConnection(null);
  }

  @Test
  public void requesterTest() throws AccessDenied, AlreadyLoggedIn,
    ServiceFailure, NotLoggedIn {
    final Connection conn = _context.createConnection(_hostName, _hostPort);
    conn.loginByPassword(entity, password.getBytes());
    assertNull(_context.getCurrentConnection());
    _context.setDefaultConnection(conn);
    _context.onCallDispatch(new CallDispatchCallback() {
      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        return conn;
      }
    });
    assertNotNull(_context.getCurrentConnection());
    assertEquals(_context.getCurrentConnection(), _context
      .getDefaultConnection());
    _context.setCurrentConnection(conn);
    assertEquals(_context.getCurrentConnection(), conn);
    _context.setDefaultConnection(null);
    _context.onCallDispatch(null);
    assertTrue(conn.logout());
    assertEquals(_context.getCurrentConnection(), conn);
    _context.setCurrentConnection(null);

    // tentativa de chamada sem conexão request setada
    conn.loginByPassword(entity, password.getBytes());
    assertNull(_context.getCurrentConnection());
    boolean failed = false;
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("a", "b") };
    try {
      _context.getOfferRegistry().findServices(props);
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
    _context.setCurrentConnection(conn);
    try {
      _context.getOfferRegistry().findServices(props);
    }
    catch (Exception e) {
      fail("A chamada com conexão setada deveria ser bem-sucedida. Exceção recebida: "
        + e);
    }
  }

  @Test
  public void callerChainTest() throws Exception {
    Connection conn = _context.createConnection(_hostName, _hostPort);
    assertNull(_context.getCallerChain());
    //atualmente os testes de interoperabilidade ja cobrem esses testes
  }

  @Test
  public void getCallerChainInDispatchTest() throws Exception {
    Connection conn = _context.createConnection(_hostName, _hostPort);
    conn.loginByPassword(entity, password.getBytes());
    _context.setDefaultConnection(conn);
    ComponentContext component = Utils.buildTestCallerChainComponent(_context);
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "OpenBusContextTest") };
    ServiceOffer offer =
      _context.getOfferRegistry().registerService(component.getIComponent(),
        props);
    ServiceOfferDesc[] services =
      _context.getOfferRegistry().findServices(props);
    assertEquals(1, services.length);
    assertNotNull(services[0]);
    offer.remove();
    _context.setDefaultConnection(null);
    conn.logout();
  }

  @Test
  public void getConnectionInDispatchTest() throws Exception {
    Connection conn = _context.createConnection(_hostName, _hostPort);
    conn.loginByPassword(entity, password.getBytes());
    _context.setDefaultConnection(conn);
    ComponentContext component = Utils.buildTestConnectionComponent(_context);
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "OpenBusContextTest") };
    ServiceOffer offer =
      _context.getOfferRegistry().registerService(component.getIComponent(),
        props);
    ServiceOfferDesc[] services =
      _context.getOfferRegistry().findServices(props);
    assertEquals(1, services.length);
    assertNotNull(services[0]);
    offer.remove();
    _context.setDefaultConnection(null);
    conn.logout();
  }

  @Test
  public void getConnectionInDispatch2Test() throws Exception {
    final Connection conn = _context.createConnection(_hostName, _hostPort);
    conn.loginByPassword(entity, password.getBytes());
    _context.setCurrentConnection(conn);
    _context.onCallDispatch(new CallDispatchCallback() {
      @Override
      public Connection dispatch(OpenBusContext context, String busid,
        String loginId, byte[] object_id, String operation) {
        return conn;
      }
    });
    ComponentContext component = Utils.buildTestConnectionComponent(_context);
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "OpenBusContextTest") };
    ServiceOffer offer =
      _context.getOfferRegistry().registerService(component.getIComponent(),
        props);
    ServiceOfferDesc[] services =
      _context.getOfferRegistry().findServices(props);
    assertEquals(1, services.length);
    assertNotNull(services[0]);
    offer.remove();
    _context.setCurrentConnection(null);
    conn.logout();
  }

  @Test
  public void joinChainTest() throws InvalidTypeForEncoding, UnknownEncoding,
    InvalidName {
    Connection conn = _context.createConnection(_hostName, _hostPort);
    assertNull(_context.getJoinedChain());
    // adiciona a chain da getCallerChain
    _context.joinChain(null);
    assertNull(_context.getJoinedChain());

    String busid = "mock";
    String target = "target";
    LoginInfo caller = new LoginInfo("a", "b");
    LoginInfo[] originators = new LoginInfo[0];

    _context.joinChain(buildFakeCallChain(busid, target, caller, originators));

    CallerChain callerChain = _context.getJoinedChain();
    assertNotNull(callerChain);
    assertEquals(busid, callerChain.busid());
    assertEquals(target, callerChain.target());
    assertEquals("a", callerChain.caller().id);
    assertEquals("b", callerChain.caller().entity);
    _context.exitChain();
  }

  @Test
  public void exitChainTest() throws InvalidTypeForEncoding, UnknownEncoding,
    InvalidName {
    Connection conn = _context.createConnection(_hostName, _hostPort);
    assertNull(_context.getJoinedChain());
    _context.exitChain();
    assertNull(_context.getJoinedChain());
    _context.joinChain(buildFakeCallChain("mock", "target", new LoginInfo("a",
      "b"), new LoginInfo[0]));
    assertNotNull(_context.getJoinedChain());
    _context.exitChain();
    assertNull(_context.getJoinedChain());
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

}
