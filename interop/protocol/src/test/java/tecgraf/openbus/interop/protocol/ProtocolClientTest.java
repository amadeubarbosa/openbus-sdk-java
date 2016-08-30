package tecgraf.openbus.interop.protocol;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
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
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.EncryptedBlockSize;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidChainCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidPublicKeyCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidTargetCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnavailableBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;

/**
 * Cliente do teste Protocol
 * 
 * @author Tecgraf
 */
public final class ProtocolClientTest {

  private static String entity;
  private static byte[] password;
  private static String domain;
  private static ORB orb;
  private static OpenBusContext context;
  private static Connection conn;
  private static Server server;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.busref;
    entity = configs.user;
    password = configs.password;
    domain = configs.domain;
    orb = ORBInitializer.initORB();
    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    context = (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    conn = context.connectByReference(busref);
  }

  @Before
  public void beforeEachTest() throws Exception {
    conn.loginByPassword(entity, password, domain);
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
    ArrayListMultimap<String, String> props = ArrayListMultimap
      .create();
    props.put("openbus.component.interface", ServerHelper.id());
    List<RemoteOffer> offers = conn.offerRegistry().findServices(props);
    if (offers.size() > 0) {
      for (RemoteOffer offer : offers) {
        try {
          if (!offer.service()._non_existent()) {
            IComponent scs = offer.service();
            Object facet = scs.getFacet(ServerHelper.id());
            if (facet != null) {
              return ServerHelper.narrow(facet);
            }
          }
        }
        catch (TRANSIENT | COMM_FAILURE e) {
          // do nothing
        }
      }
    }
    throw new IllegalStateException("couldn't find a responsive offer");
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
