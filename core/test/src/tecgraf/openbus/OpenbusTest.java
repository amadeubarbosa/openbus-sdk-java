package tecgraf.openbus;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.UserException;

import scs.core.IReceptaclesHelper;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.InvalidCredentialException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.util.CryptoUtils;

public class OpenbusTest {
  protected static Properties props;

  private String userLogin;
  private String userPassword;
  protected String hostName;
  protected int hostPort;

  private String testKey;
  private String acsCertificate;
  private String testCertificateName;

  private String keyStorePath;
  private String keyStorePassword;
  private String keyStoreTestEntityName;
  private String keyStoreTestAlias;
  private String keyStoreTestPassword;
  private String keyStoreOpenBusAlias;

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

    this.userLogin = props.getProperty("User.Login");
    this.userPassword = props.getProperty("User.Password");
    this.hostName = props.getProperty("Host.Name");
    this.hostPort = Integer.valueOf(props.getProperty("Host.Port"));

    this.testKey = props.getProperty("Server.Key");
    this.acsCertificate = props.getProperty("Acs.Certificate");
    this.testCertificateName = props.getProperty("Server.EntityName");

    this.keyStorePath = props.getProperty("KeyStore.Path");
    this.keyStorePassword = props.getProperty("KeyStore.Password");
    this.keyStoreTestEntityName =
      props.getProperty("KeyStore.Server.EntityName");
    this.keyStoreTestAlias = props.getProperty("KeyStore.Server.Alias");
    this.keyStoreTestPassword = props.getProperty("KeyStore.Server.Password");
    this.keyStoreOpenBusAlias = props.getProperty("KeyStore.OpenBus.Alias");
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
    openbus.destroy();
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
   * Testa o connect sendo executado duas vezes.
   * 
   * @throws OpenBusException
   */
  @Test(expected = ACSLoginFailureException.class)
  public void connectByPasswordLoginTwice() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect(userLogin, userPassword);
      Assert.assertNotNull(registryService);
      registryService = openbus.connect(userLogin, userPassword);
    }
    finally {
      Assert.assertTrue(openbus.disconnect());
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
      registryService = openbus.connect("null", "nullnull");
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
   * Testa o connect sendo executado duas vezes.
   * 
   * @throws OpenBusException
   * @throws Exception
   */
  @Test(expected = ACSLoginFailureException.class)
  public void connectByCertificateTwice() throws OpenBusException, Exception {
    RSAPrivateKey key = CryptoUtils.readPrivateKey(testKey);
    X509Certificate acsCert = CryptoUtils.readCertificate(acsCertificate);
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService =
      openbus.connect(testCertificateName, key, acsCert);
    Assert.assertNotNull(registryService);
    try {
      registryService = openbus.connect(testCertificateName, key, acsCert);
    }
    finally {
      Assert.assertTrue(openbus.disconnect());
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
   * Testa o connect passando um <i>entityName</i> inválido.
   * 
   * @throws OpenBusException
   * @throws Exception
   */
  @Test(expected = ACSLoginFailureException.class)
  public void connectByCertificateInlaidEntityName() throws OpenBusException,
    Exception {
    RSAPrivateKey key = CryptoUtils.readPrivateKey(testKey);
    X509Certificate acsCert = CryptoUtils.readCertificate(acsCertificate);
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = null;
    try {
      registryService = openbus.connect("INVALID", key, acsCert);
    }
    finally {
      Assert.assertNull(registryService);
      Assert.assertFalse(openbus.disconnect());
    }
  }

  /**
   * Testa o connect utilizando um keystore.
   * 
   * @throws OpenBusException
   */
  @Test
  public void connectByKeyStoreCertificate() throws OpenBusException {
    InputStream keyStoreInputStream =
      this.getClass().getResourceAsStream(this.keyStorePath);
    Assert.assertNotNull(keyStoreInputStream);
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService =
      openbus.connect(this.keyStoreTestEntityName, keyStoreInputStream,
        this.keyStorePassword.toCharArray(), this.keyStoreTestAlias,
        this.keyStoreTestPassword.toCharArray(), this.keyStoreOpenBusAlias);
    Assert.assertNotNull(registryService);
    Assert.assertTrue(openbus.disconnect());
  }

  /**
   * Testa o connect utilizando um keystore.
   * 
   * @throws OpenBusException
   */
  @Test(expected = IllegalArgumentException.class)
  public void connectByKeyStoreCertificatePathNull() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService =
      openbus.connect(this.keyStoreTestEntityName, null, this.keyStorePassword
        .toCharArray(), this.keyStoreTestAlias, this.keyStoreTestPassword
        .toCharArray(), this.keyStoreOpenBusAlias);
    Assert.assertNotNull(registryService);
    Assert.assertTrue(openbus.disconnect());
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
   * Testa o connect passando a credencial inválida.
   * 
   * @throws OpenBusException
   */
  @Test(expected = InvalidCredentialException.class)
  public void connectByCredentialInvalidCredencial() throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    // Fazendo o Login de um usuário para receber uma credencial.
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Credential credential = openbus.getCredential();
    Assert.assertNotNull(credential);
    Assert.assertTrue(openbus.disconnect());
    registryService = null;
    try {
      registryService = openbus.connect(credential);
    }
    finally {
      Assert.assertNull(registryService);
    }

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
   * Testa se o método getAccessControlService retorna um objeto
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test
  public void getAccessControlService() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getAccessControlService());
    Assert.assertTrue(openbus.disconnect());
    Assert.assertNull(openbus.getAccessControlService());
  }

  /**
   * Testa se o método getRegistryService retorna um objeto
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test
  public void getRegistryService() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = openbus.connect(userLogin, userPassword);
    Assert.assertNotNull(registryService);
    Assert.assertNotNull(openbus.getRegistryService());
    Assert.assertTrue(openbus.disconnect());
    Assert.assertNull(openbus.getRegistryService());
  }

  /**
   * Testa se o método isInterceptable está funcionando corretamente.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test
  public void isInterceptable() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    String repID = IReceptaclesHelper.id();
    String methodName = "getConnections";

    openbus.setInterceptable(repID, methodName, true);
    Assert.assertTrue(openbus.isInterceptable(repID, methodName));
    openbus.setInterceptable(repID, methodName, false);
    Assert.assertFalse(openbus.isInterceptable(repID, methodName));
    openbus.setInterceptable(repID, methodName, true);
    Assert.assertTrue(openbus.isInterceptable(repID, methodName));
  }

  /**
   * Verifica se uma credencial nula é retornada como credencial interceptada
   * quando não estamos na thread de uma chamada remota.
   */
  @Test
  public void getInterceptedCredential() {
    Openbus openbus = Openbus.getInstance();
    Assert.assertNull(openbus.getInterceptedCredential());
  }
}