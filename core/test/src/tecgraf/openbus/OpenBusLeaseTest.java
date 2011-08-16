package tecgraf.openbus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.UserException;

import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.lease.LeaseExpiredCallback;

public final class OpenBusLeaseTest {
  private static Properties props;
  private String hostName;
  private int hostPort;
  private String userLogin;
  private String userPassword;

  /**
   * Construtor
   * 
   * @throws IOException
   */
  public OpenBusLeaseTest() throws IOException {
    // Carregando o arquivo de configuração
    Properties props = new Properties();
    InputStream in =
      this.getClass().getResourceAsStream("/AllTests.properties");
    props.load(in);
    in.close();

    this.userLogin = props.getProperty("User.Login");
    this.userPassword = props.getProperty("User.Password");
    this.hostName = props.getProperty("Host.Name");
    this.hostPort = Integer.valueOf(props.getProperty("Host.Port"));
  }

  /**
   * Este método é chamado antes de todos os testCases.
   */
  @BeforeClass
  public static void beforeClass() {
    props = new Properties();
    props.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    props.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
  }

  /**
   * Este método é chamado antes de cada testCase.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Before
  public void beforeTest() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    openbus.init(null, props, hostName, hostPort);
  }

  /**
   * Este método é chamado depois de cada testCase.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @After
  public void afterTest() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    openbus.disconnect();
    openbus.destroy();
  }

  /**
   * Testa se a callback de expiração da credencial é corretamente acionada.
   * 
   * @throws OpenBusException
   */
  @Test(timeout = 4 * 60 * 1000)
  public void credentialExpired() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    class LeaseExpiredCallbackImpl implements LeaseExpiredCallback {
      private volatile boolean expired;

      LeaseExpiredCallbackImpl() {
        this.expired = false;
      }

      public void expired() {
        this.expired = true;
      }

      public boolean isExpired() {
        return this.expired;
      }
    }
    LeaseExpiredCallbackImpl callback = new LeaseExpiredCallbackImpl();
    openbus.setLeaseExpiredCallback(callback);
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    IAccessControlService acs = openbus.getAccessControlService();
    acs.logout(openbus.getCredential());
    while (!callback.isExpired()) {
      ;
    }
  }

  /**
   * Testa a utilização de uma callback responsável por reconectar após a
   * expiração da credencial. O cadastro dessa lease acontece antes do método
   * connect.
   * 
   * @throws OpenBusException
   */
  @Test(timeout = 4 * 60 * 1000)
  public void addLeaseExpiredCbBeforeConnect() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    class LeaseExpiredCallbackImpl implements LeaseExpiredCallback {
      private volatile boolean reconnected;

      LeaseExpiredCallbackImpl() {
        this.reconnected = false;
      }

      public void expired() {
        Openbus openbus = Openbus.getInstance();
        try {
          openbus.connect(userLogin, userPassword);
          this.reconnected = true;
        }
        catch (OpenBusException e) {
          this.reconnected = false;
        }
      }

      public boolean isReconnected() {
        return this.reconnected;
      }
    }
    LeaseExpiredCallbackImpl callback = new LeaseExpiredCallbackImpl();
    openbus.setLeaseExpiredCallback(callback);
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Credential credential = openbus.getCredential();
    Assert.assertNotNull(registryService);
    IAccessControlService acs = openbus.getAccessControlService();
    acs.logout(openbus.getCredential());
    while (!callback.isReconnected()) {
      ;
    }
    // O Openbus foi reconectado
    Credential newCredential = openbus.getCredential();
    Assert.assertFalse(credential.identifier.equals(newCredential.identifier));
  }

  /**
   * Testa a utilização de uma callback responsável por reconectar ao barramento
   * após a expiração da credencial. O cadastro dessa lease acontece depois do
   * método connect.
   * 
   * @throws OpenBusException
   */
  @Test(timeout = 4 * 60 * 1000)
  public void addLeaseExpiredCbAfterConnect() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    class LeaseExpiredCallbackImpl implements LeaseExpiredCallback {
      private volatile boolean reconnected;

      LeaseExpiredCallbackImpl() {
        this.reconnected = false;
      }

      public void expired() {
        Openbus openbus = Openbus.getInstance();
        try {
          openbus.connect(userLogin, userPassword);
          this.reconnected = true;
        }
        catch (OpenBusException e) {
          this.reconnected = false;
        }
      }

      public boolean isReconnected() {
        return this.reconnected;
      }
    }
    LeaseExpiredCallbackImpl callback = new LeaseExpiredCallbackImpl();
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    openbus.setLeaseExpiredCallback(callback);
    Credential credential = openbus.getCredential();
    Assert.assertNotNull(registryService);
    IAccessControlService acs = openbus.getAccessControlService();
    acs.logout(openbus.getCredential());
    while (!callback.isReconnected()) {
      ;
    }
    // O Openbus foi reconectado
    Credential newCredential = openbus.getCredential();
    Assert.assertFalse(credential.identifier.equals(newCredential.identifier));
  }
}
