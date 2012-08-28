package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.NotLoggedIn;
import tecgraf.openbus.util.Utils;

public final class ConnectionManagerTest {
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

  //  @Test
  //  public void getDispatcherTest() throws AccessDenied, AlreadyLoggedIn,
  //    ServiceFailure, NotLoggedIn {
  //    Connection conn = _context.createConnection(_hostName, _hostPort);
  //    conn.loginByPassword(entity, password.getBytes());
  //
  //    Connection conn2 = _context.createConnection(_hostName, _hostPort);
  //    conn2.loginByPassword(entity, password.getBytes());
  //
  //    _context.setDefaultConnection(conn);
  //    assertNull(_context.getDispatcher(conn.busid()));
  //
  //    _context.setCurrentConnection(conn2);
  //    assertNull(_context.getDispatcher(conn.busid()));
  //
  //    _context.setDispatcher(conn2);
  //    assertEquals(_context.getDispatcher(conn.busid()), conn2);
  //
  //    _context.clearDispatcher(conn.busid());
  //    assertNull(_context.getDispatcher(conn2.busid()));
  //
  //    _context.setDispatcher(conn2);
  //    assertTrue(conn2.logout());
  //
  //    assertNotNull(_context.getDispatcher(conn.busid()));
  //    assertEquals(_context.getDispatcher(conn.busid()), conn2);
  //    _context.setCurrentConnection(null);
  //    assertTrue(conn.logout());
  //    _context.setDefaultConnection(null);
  //    _context.clearDispatcher(conn.busid());
  //    assertNull(_context.getDispatcher(conn.busid()));
  //  }

  //  @Test
  //  public void clearDispatcherTest() throws NotLoggedIn, AccessDenied,
  //    AlreadyLoggedIn, ServiceFailure {
  //    Connection conn = _context.createConnection(_hostName, _hostPort);
  //    Connection conn2 = _context.createConnection(_hostName, _hostPort);
  //    conn.loginByPassword(entity, password.getBytes());
  //    conn2.loginByPassword(entity, password.getBytes());
  //    Connection removed = _context.clearDispatcher(conn.busid());
  //    assertNull(removed);
  //    _context.setDefaultConnection(conn);
  //    _context.setDispatcher(conn2);
  //    removed = _context.clearDispatcher(conn.busid());
  //    assertEquals(removed, conn2);
  //    assertTrue(conn.logout());
  //    _context.setDefaultConnection(conn2);
  //    assertTrue(conn2.logout());
  //    _context.setDefaultConnection(null);
  //  }

  //  @Test
  //  public void setDispatcherTest() throws NotLoggedIn, AccessDenied,
  //    AlreadyLoggedIn, ServiceFailure {
  //    Connection conn = _context.createConnection(_hostName, _hostPort);
  //    boolean failed = false;
  //    try {
  //      _context.setDispatcher(null);
  //    }
  //    catch (NullPointerException e) {
  //      failed = true;
  //    }
  //    assertTrue(failed);
  //    conn.loginByPassword(entity, password.getBytes());
  //
  //    Connection conn2 = _context.createConnection(_hostName, _hostPort);
  //    conn2.loginByPassword(entity, password.getBytes());
  //    _context.setDefaultConnection(conn);
  //    assertNull(_context.getDispatcher(conn.busid()));
  //    _context.setCurrentConnection(conn);
  //    assertNull(_context.getDispatcher(conn.busid()));
  //    _context.setDispatcher(conn2);
  //    assertEquals(_context.getDispatcher(conn.busid()), conn2);
  //    _context.setCurrentConnection(conn2);
  //    assertTrue(conn2.logout());
  //    assertNotNull(_context.getDispatcher(conn.busid()));
  //    assertEquals(_context.getDispatcher(conn.busid()), conn2);
  //    _context.clearDispatcher(conn.busid());
  //    assertNull(_context.getDispatcher(conn.busid()));
  //    _context.setCurrentConnection(null);
  //    assertTrue(conn.logout());
  //    _context.setDefaultConnection(null);
  //  }

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
    assertNull(_context.getCurrentConnection());
    _context.setCurrentConnection(conn);
    assertEquals(_context.getCurrentConnection(), conn);
    _context.setDefaultConnection(null);
    _context.onCallDispatch(null);
    assertTrue(conn.logout());
    assertEquals(_context.getCurrentConnection(), conn);
    _context.setCurrentConnection(null);

    // tentativa de chamada sem threadrequester setado
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
    // tentativa com threadrequester setado
    _context.setCurrentConnection(conn);
    try {
      _context.getOfferRegistry().findServices(props);
    }
    catch (Exception e) {
      fail("A chamada com ThreadRequester setado deveria ser bem-sucedida. Exceção recebida: "
        + e);
    }
  }

  @Test
  public void callerChainTest() throws Exception {
    Connection conn = _context.createConnection(_hostName, _hostPort);
    assertNull(_context.getCallerChain());
    //TODO: adicionar testes para caso exista uma callerchain ou os testes de interoperabilidade ja cobrem isso de forma suficiente?
  }

  @Test
  public void JoinChainTest() {
    Connection conn = _context.createConnection(_hostName, _hostPort);
    assertNull(_context.getJoinedChain());
    // adiciona a chain da getCallerChain
    _context.joinChain(null);
    assertNull(_context.getJoinedChain());

    //TODO testar caso em que a chain da getCallerChain não é vazia
    //TODO comparar assinatura do callerchainimpl com a implementação CSHARP

    _context.joinChain(new CallerChainImpl("mock", "target", new LoginInfo("a",
      "b"), new LoginInfo[0], new SignedCallChain(new byte[256], new byte[0])));

    CallerChain callerChain = _context.getJoinedChain();
    assertNotNull(callerChain);
    assertEquals("mock", callerChain.busid());
    assertEquals("a", callerChain.caller().id);
    assertEquals("b", callerChain.caller().entity);
    _context.exitChain();
  }

  @Test
  public void ExitChainTest() {
    Connection conn = _context.createConnection(_hostName, _hostPort);
    assertNull(_context.getJoinedChain());
    _context.exitChain();
    assertNull(_context.getJoinedChain());
    _context.joinChain(new CallerChainImpl("mock", "target", new LoginInfo("a",
      "b"), new LoginInfo[0], new SignedCallChain(new byte[256], new byte[0])));
    _context.exitChain();
    assertNull(_context.getJoinedChain());
  }
}
