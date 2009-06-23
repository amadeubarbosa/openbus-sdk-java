package tecgraf.openbus;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.logging.Level;

import openbusidl.acs.Credential;
import openbusidl.rs.IRegistryService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.UserException;

import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.util.CryptoUtils;
import tecgraf.openbus.util.Log;

public class OpenbusTest {
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

    this.userLogin = props.getProperty("userLogin");
    this.userPassword = props.getProperty("userPassword");
    this.hostName = props.getProperty("hostName");
    this.hostPort = Integer.valueOf(props.getProperty("hostPort"));

    this.testKey = props.getProperty("testKey");
    this.acsCertificate = props.getProperty("ACServiceCert");
    this.testCertificateName = props.getProperty("testCertificateName");
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

    Log.setLogsLevel(Level.WARNING);
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
    openbus.resetAndInitialize(null, props, hostName, hostPort);
  }

  /**
   * Testa o connect passando usuário e senha válido
   * 
   * @throws OpenBusException
   */
  @Test
  public void connectByPassword() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa o connect passando usuário e senha nulos.
   * 
   * @throws OpenBusException
   */
  @Test(expected = IllegalArgumentException.class)
  public void connectByPasswordLoginNull() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect(null, null);
    }
    finally {
      Assert.assertNull(registryService);
      Assert.assertFalse(openbus.disconnect());
    }
  }

  /**
   * Testa o connect passando usuário e senha inválido.
   * 
   * @throws OpenBusException
   */
  @Test(expected = ACSLoginFailureException.class)
  public void connectByPasswordInvalidLogin() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect("null", "null");
    }
    finally {
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
  @Test
  public void connectByCertificate() throws OpenBusException, Exception {
    RSAPrivateKey key = CryptoUtils.readPrivateKey(testKey);
    X509Certificate acsCert = CryptoUtils.readCertificate(acsCertificate);
    Openbus openbus = Openbus.getInstance();
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
  @Test(expected = IllegalArgumentException.class)
  public void connectByCertificateNullKey() throws OpenBusException, Exception {
    X509Certificate acsCert = CryptoUtils.readCertificate(acsCertificate);
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect(testCertificateName, null, acsCert);
    }
    finally {
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
  @Test(expected = IllegalArgumentException.class)
  public void connectByCertificateNullACSCertificate() throws OpenBusException,
    Exception {
    RSAPrivateKey Key = CryptoUtils.readPrivateKey(testKey);
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect(testCertificateName, Key, null);
    }
    finally {
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
  @Test
  public void connectByCredential() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
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
  @Test(expected = IllegalArgumentException.class)
  public void connectByCredentialNullCredential() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    Credential credential = openbus.getCredential();
    Assert.assertNull(credential);
    IRegistryService registryService = openbus.connect(credential);
    Assert.assertNotNull(registryService);
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa se o método isConnected está funcionando corretamente.
   * 
   * @throws OpenBusException
   * @throws ACSLoginFailureException
   */
  @Test
  public void isConnected() throws ACSLoginFailureException, OpenBusException {
    Openbus openbus = Openbus.getInstance();
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
  @Test
  public void disconnected() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    Assert.assertFalse(openbus.disconnect());
  }

  /**
   * Testa se o método getORB retorna um objeto
   * 
   * @throws OpenBusException
   */
  @Test
  public void getOrb() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
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
  @Test
  public void getRootPOA() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getRootPOA());
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa se o método getAccessControlServer retorna um objeto
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test
  public void getAcs() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getAccessControlService());
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa se o método getRegistryServer retorna um objeto
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test
  public void getRegistryServer() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getRegistryService());
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa se o método getSessionService não retorna um objeto. Lembrando que
   * este teste espera que o Serviço de Sessão não tenha sido levantado no
   * barramento.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test
  public void getSessionService() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNull(openbus.getSessionService());
    Assert.assertTrue(openbus.disconnect());
  }

}
