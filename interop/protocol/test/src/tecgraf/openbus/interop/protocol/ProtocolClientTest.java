package tecgraf.openbus.interop.protocol;

import java.util.Arrays;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.TRANSIENT;

import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.EncryptedBlockSize;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidChainCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidPublicKeyCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidTargetCode;
import tecgraf.openbus.core.v2_0.services.access_control.NoCredentialCode;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnavailableBusCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.security.Cryptography;

/**
 * Cliente do teste Protocol
 * 
 * @author Tecgraf
 */
public final class ProtocolClientTest {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static ORB orb;
  private static OpenBusContext context;
  private static Connection conn;
  private static Server server;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    entity = properties.getProperty("entity.name");
    password = properties.getProperty("entity.password");
    orb = ORBInitializer.initORB();
    context = (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    conn = context.createConnection(host, port);
  }

  @Before
  public void beforeEachTest() throws Exception {
    conn.loginByPassword(entity, entity.getBytes());
    context.setDefaultConnection(conn);
    server = findServer();
  }

  @After
  public void afterEachTest() {
    try {
      conn.logout();
    }
    catch (ServiceFailure e) {
      // do nothing
    }
    context.setDefaultConnection(null);
  }

  @AfterClass
  public static void oneTimeTearDown() throws Exception {
    orb.shutdown(false);
  }

  private static Server findServer() throws ServiceFailure {
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty(
        "openbus.component.interface", ServerHelper.id()) };
    ServiceOfferDesc[] offers = context.getOfferRegistry().findServices(props);
    if (offers.length > 0) {
      for (ServiceOfferDesc offer : offers) {
        try {
          if (!offer.service_ref._non_existent()) {
            IComponent scs = offer.service_ref;
            Object facet = scs.getFacet(ServerHelper.id());
            if (facet != null) {
              return ServerHelper.narrow(facet);
            }
          }
        }
        catch (TRANSIENT e) {
          // do nothing
        }
        catch (COMM_FAILURE e) {
          // do nothing
        }
      }
    }
    throw new IllegalStateException("couldn't find a responsive offer");
  }

  @Test
  public void resetCredentialTest() {
    try {
      String target = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX";
      int session = 2 ^ 32 - 1;
      byte[] secret = new byte[16];
      Arrays.fill(secret, (byte) '\171');
      server.ResetCredential(target, session, secret);
    }
    catch (NO_PERMISSION e) {
      if (InvalidTargetCode.value == e.minor
        && e.completed.equals(CompletionStatus.COMPLETED_NO)) {
        // success!!
        return;
      }
      throw e;
    }
  }

  @Test
  public void resetCredentialWithChallengeTest() {
    try {
      int session = 2 ^ 32 - 1;
      byte[] challenge = new byte[EncryptedBlockSize.value];
      Arrays.fill(challenge, (byte) 0x01);
      server.ResetCredentialWithChallenge(session, challenge);
    }
    catch (NO_PERMISSION e) {
      if (InvalidRemoteCode.value == e.minor
        && e.completed.equals(CompletionStatus.COMPLETED_NO)) {
        // success!!
        return;
      }
      throw e;
    }
  }

  private void fireNoPermission(int raise, int expect) {
    try {
      server.RaiseNoPermission(raise);
    }
    catch (NO_PERMISSION e) {
      if (expect == e.minor
        && e.completed.equals(CompletionStatus.COMPLETED_NO)) {
        // success!!
        return;
      }
      throw e;
    }
  }

  @Test
  public void noPermissionZeroTest() {
    fireNoPermission(0, 0);
  }

  @Test
  public void noPermissionInvalidCredentialTest() {
    fireNoPermission(InvalidCredentialCode.value, InvalidRemoteCode.value);
  }

  @Test
  public void noPermissionInvalidChainTest() {
    fireNoPermission(InvalidChainCode.value, InvalidChainCode.value);
  }

  @Test
  public void noPermissionUnverifiedLoginTest() {
    fireNoPermission(UnverifiedLoginCode.value, UnverifiedLoginCode.value);
  }

  @Test
  public void noPermissionUnknownBusTest() {
    fireNoPermission(UnknownBusCode.value, UnknownBusCode.value);
  }

  @Test
  public void noPermissionInvalidPubKeyTest() {
    fireNoPermission(InvalidPublicKeyCode.value, InvalidPublicKeyCode.value);
  }

  @Test
  public void noPermissionNoCredentialTest() {
    fireNoPermission(NoCredentialCode.value, NoCredentialCode.value);
  }

  @Test
  public void noPermissionNoLoginTest() {
    fireNoPermission(NoLoginCode.value, InvalidRemoteCode.value);
  }

  @Test
  public void noPermissionInvalidRemoteTest() {
    fireNoPermission(InvalidRemoteCode.value, InvalidRemoteCode.value);
  }

  @Test
  public void noPermissionUnavailableBusTest() {
    fireNoPermission(UnavailableBusCode.value, InvalidRemoteCode.value);
  }

  @Test
  public void noPermissionInvalidTargetTest() {
    fireNoPermission(InvalidTargetCode.value, InvalidRemoteCode.value);
  }

  @Test
  public void noPermissionInvalidLoginTest() {
    fireNoPermission(InvalidLoginCode.value, InvalidRemoteCode.value);
  }

}
