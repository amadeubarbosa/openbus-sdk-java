package tecgraf.openbus;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.logging.Level;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.omg.CORBA.UserException;

import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.lease.LeaseExpiredCallback;
import tecgraf.openbus.util.CryptoUtils;
import tecgraf.openbus.util.Log;

public class OpenbusTest extends TestCase{
  private static Properties props;

  private String userLogin;
  private String userPassword;
  private String hostName;
  private int hostPort;

  private String testKey;
  private String acsCertificate;
  private String testCertificateName;

  /**
   * Construtor
   * 
   * @throws IOException
   */
  public OpenbusTest() throws IOException {
    // Carregando o arquivo de configuração
    Properties props = new Properties();
    InputStream in =
      this.getClass().getResourceAsStream("/AllTests.properties");
    props.load(in);
    in.close();

    this.userLogin = props.getProperty("userLogin") + System.currentTimeMillis();
    
    this.userPassword = props.getProperty("userPassword");
    this.hostName = props.getProperty("hostName");
    this.hostPort = Integer.valueOf(props.getProperty("hostPort"));

    this.testKey = props.getProperty("testKey");
    this.acsCertificate = props.getProperty("ACServiceCert");
    this.testCertificateName = props.getProperty("testCertificateName");
  }
  
  Openbus openbus;

  /**
   * Este método é chamado antes de cada testCase.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  public void setUp() throws OpenBusException, UserException {
	props = new Properties();
	props.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
	props.setProperty("org.omg.CORBA.ORBSingletonClass",
	      "org.jacorb.orb.ORBSingleton");

	Log.setLogsLevel(Level.FINEST);
	  
	openbus = Openbus.getInstance();
    //openbus.init(null, props, hostName, hostPort);
    openbus.initWithFaultTolerance(null, props, hostName, hostPort);
  }
  

  /**
   * Este método é chamado depois de cada testCase.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  public void tearDown() throws OpenBusException, UserException {
    openbus.destroy();
  }

  /**
   * Testa o connect passando usuário e senha válido
   * 
   * @throws OpenBusException
   */
  public void testConnectByPassword() throws OpenBusException {
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa o connect passando usuário e senha nulos.
   * 
   * @throws OpenBusException
   */
  public void testConnectByPasswordLoginNull() throws OpenBusException {
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect(null, null);
    } catch (Exception e) {
    	Assert.assertNull(registryService);
        Assert.assertFalse(openbus.disconnect());
	}
    
  }

  /**
   * Testa o connect passando usuário e senha inválido.
   * 
   * @throws OpenBusException
   */
  public void testConnectByPasswordInvalidLogin() throws OpenBusException {
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect("null", "null");
    }catch (Exception e) {
      Assert.assertNull(registryService);
      Assert.assertFalse(openbus.disconnect());
    }
  }

  /**
   * Testa o connect passando o certificado.
   * 
   * @throws OpenBusException
   * @throws Exception
   */
  public void testConnectByCertificate() throws OpenBusException, Exception {
    RSAPrivateKey key = CryptoUtils.readPrivateKey(testKey);
    X509Certificate acsCert = CryptoUtils.readCertificate(acsCertificate);
    IRegistryService registryService =
      openbus.connect(testCertificateName, key, acsCert);
    Assert.assertNotNull(registryService);
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa o connect passando a chave nula.
   * 
   * @throws OpenBusException
   * @throws Exception
   */
  public void testConnectByCertificateNullKey() throws OpenBusException, Exception {
    X509Certificate acsCert = CryptoUtils.readCertificate(acsCertificate);
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect(testCertificateName, null, acsCert);
    }catch (Exception e) {
      Assert.assertNull(registryService);
      Assert.assertFalse(openbus.disconnect());
    }
  }

  /**
   * Testa o connect passando o certificado do ACS nulo.
   * 
   * @throws OpenBusException
   * @throws Exception
   */
  public void testConnectByCertificateNullACSCertificate() throws OpenBusException,
    Exception {
    RSAPrivateKey Key = CryptoUtils.readPrivateKey(testKey);
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect(testCertificateName, Key, null);
    }catch (Exception e) {
      Assert.assertNull(registryService);
      Assert.assertFalse(openbus.disconnect());
    }
  }

  /**
   * Testa o connect por credencial. Este teste faz um loginByPassword para
   * criar uma credencial. Depois faz um loginByCredential passando a credencial
   * criada como parâmetro.
   * 
   * @throws OpenBusException
   */
  public void testConnectByCredential() throws OpenBusException {
    Credential credential = openbus.getCredential();
    Assert.assertNull(credential);
    // Fazendo o Login de um usuário para receber uma credencial.
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    credential = openbus.getCredential();
    Assert.assertNotNull(credential);
    registryService = null;
    registryService = openbus.connect(credential);
    Assert.assertNotNull(registryService);
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa o connect passando uma credencial inválida recebida pela classe
   * Openbus.
   * 
   * @throws OpenBusException
   */
  public void testConnectByCredentialNullCredential() throws OpenBusException {
    Credential credential = openbus.getCredential();
    Assert.assertNull(credential);
    IRegistryService registryService = null;
    try {
    	registryService = openbus.connect(credential);
	} catch (Exception e) {
		Assert.assertNull(registryService);
	    Assert.assertFalse(openbus.disconnect());
	}
    
    
  }

  /**
   * Testa se o método isConnected está funcionando corretamente.
   * 
   * @throws OpenBusException
   * @throws ACSLoginFailureException
   */
  public void testIsConnected() throws ACSLoginFailureException, OpenBusException {
    Assert.assertFalse(openbus.isConnected());
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertTrue(openbus.isConnected());
    Assert.assertTrue(openbus.disconnect());
    Assert.assertFalse(openbus.isConnected());
  }

  /**
   * Testa se o disconect retorna false caso o usuário não esteja conectado
   * 
   * @throws OpenBusException
   */
  public void testDisconnected() throws OpenBusException {
    Assert.assertFalse(openbus.disconnect());
  }

  /**
   * Testa se o método getORB retorna um objeto
   * 
   * @throws OpenBusException
   */
  public void testGetOrb() throws OpenBusException {
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getORB());
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa se o método getRootPOA retorna um objeto
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  public void testGetRootPOA() throws OpenBusException, UserException {
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getRootPOA());
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa se o método getAccessControlService retorna um objeto
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  public void testGetAccessControlService() throws OpenBusException, UserException {
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getAccessControlService());
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa se o método getRegistryService retorna um objeto
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  public void testGetRegistryService() throws OpenBusException, UserException {
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getRegistryService());
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa se o método getSessionService retorna um objeto
   * 
   * @throws OpenBusException
   * @throws UserException
   */
/*  
  public void testGetSessionService() throws OpenBusException, UserException {
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getSessionService());
    Assert.assertTrue(openbus.disconnect());
  }
*/
  /**
   * Testa se a callback de expiração da credencial é corretamente acionada.
   * 
   * @throws OpenBusException
   */
  ///@Test(timeout = 4 * 60 * 1000)
/*  
  public void testCredentialExpired() throws OpenBusException {
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
*/
  /**
   * Testa a utilização de uma callback responsável por reconectar após a
   * expiração da credencial. O cadastro dessa lease acontece antes do método
   * connect.
   * 
   * @throws OpenBusException
   */
  //@Test(timeout = 4 * 60 * 1000)
/*  
  public void testAddLeaseExpiredCbBeforeConnect() throws OpenBusException {
    class LeaseExpiredCallbackImpl implements LeaseExpiredCallback {
      private volatile boolean reconnected;

      LeaseExpiredCallbackImpl() {
        this.reconnected = false;
      }

      public void expired() {
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
    //O Openbus foi reconectado
    Credential newCredential = openbus.getCredential();
    Assert.assertFalse(credential.identifier.equals(newCredential.identifier));
  }
*/
  /**
   * Testa a utilização de uma callback responsável por reconectar ao barramento
   * após a expiração da credencial. O cadastro dessa lease acontece depois do
   * método connect.
   * 
   * @throws OpenBusException
   */
  //@Test(timeout = 4 * 60 * 1000)
/*  
  public void testAddLeaseExpiredCbAfterConnect() throws OpenBusException {
    class LeaseExpiredCallbackImpl implements LeaseExpiredCallback {
      private volatile boolean reconnected;

      LeaseExpiredCallbackImpl() {
        this.reconnected = false;
      }

      public void expired() {
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
    //O Openbus foi reconectado
    Credential newCredential = openbus.getCredential();
    Assert.assertFalse(credential.identifier.equals(newCredential.identifier));
  }
*/

}
