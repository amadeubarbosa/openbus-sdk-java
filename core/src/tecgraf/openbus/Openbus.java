/*
 * $Id$
 */
package tecgraf.openbus;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jacorb.orb.policies.RelativeRoundtripTimeoutPolicy;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SetOverrideType;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scs.core.ConnectionDescription;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidName;
import tecgraf.openbus.authenticators.Authenticator;
import tecgraf.openbus.authenticators.CertificateAuthenticator;
import tecgraf.openbus.authenticators.LoginPasswordAuthenticator;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.CredentialHelper;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlServiceHelper;
import tecgraf.openbus.core.v1_05.access_control_service.ILeaseProvider;
import tecgraf.openbus.core.v1_05.access_control_service.ILeaseProviderHelper;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryServiceHelper;
import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.AlreadyInitializedException;
import tecgraf.openbus.exception.CORBAException;
import tecgraf.openbus.exception.InvalidCredentialException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.PKIException;
import tecgraf.openbus.exception.ServiceUnavailableException;
import tecgraf.openbus.fault_tolerance.v1_05.IFaultTolerantService;
import tecgraf.openbus.fault_tolerance.v1_05.IFaultTolerantServiceHelper;
import tecgraf.openbus.interceptors.ClientInitializer;
import tecgraf.openbus.interceptors.CredentialValidationPolicy;
import tecgraf.openbus.interceptors.ServerInitializer;
import tecgraf.openbus.lease.LeaseExpiredCallback;
import tecgraf.openbus.lease.LeaseRenewer;
import tecgraf.openbus.util.Utils;

/**
 * API de acesso a um barramento OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Openbus {
  /**
   * Um valor inválido para a porta do Serviço de Controle de Acesso.
   */
  private static final int INVALID_PORT = -1;
  /**
   * Um valor inválido para o slot da credencial.
   */
  private static final int INVALID_CREDENTIAL_SLOT = -1;
  /**
   * A instância única do barramento.
   */
  private static Openbus instance;
  /**
   * O ORB.
   */
  private ORB orb;
  /**
   * O RootPOA.
   */
  private POA rootPOA;
  /**
   * Prefixo do nome da propriedade do(s) inicializador(es) do ORB.
   */
  private static final String ORB_INITIALIZER_PROPERTY_NAME_PREFIX =
    "org.omg.PortableInterceptor.ORBInitializerClass.";
  /**
   * O host do Serviço de Controle de Acesso.
   */
  private String host;
  /**
   * A porta do Serviço de Controle de Acesso.
   */
  private int port;
  /**
   * O Serviço de Controle de Acesso.
   */
  private IAccessControlService acs;
  /**
   * Interface ILeaseProvider do Serviço de Controle de Acesso.
   */
  private ILeaseProvider lp;

  /**
   * Interface IFaultTolerantService do Serviço de Controle de Acesso.
   */
  private IFaultTolerantService ft;
  /**
   * 
   * Interface ILeaseProvider do Serviço de Controle de Acesso.
   */
  private IComponent ic;
  /**
   * O renovador do <i>lease</i>.
   */
  private LeaseRenewer leaseRenewer;
  /**
   * <i>Callback</i> para a notificação de que um <i>lease</i> expirou.
   */
  private LeaseExpiredCallback leaseExpiredCallback;
  /**
   * Credencial recebida ao se conectar ao barramento.
   */
  private Credential credential;
  /**
   * A credencial da entidade, válida apenas na <i>thread</i> corrente.
   */
  private ThreadLocal<Credential> threadLocalCredential;
  /**
   * O slot da credencial da requisição.
   */
  private int requestCredentialSlot;
  /**
   * Indica se o mecanismo de tolerancia a falhas esta ativado.
   */
  private boolean faultToleranceEnabled;

  /**
   * Mantém a lista de métodos a serem liberados por interface.
   */
  private Map<String, Set<String>> ifaceMap;

  /**
   * A política de validação das credenciais obtidas pelo interceptador
   * servidor.
   */
  private CredentialValidationPolicy credentialValidationPolicy;

  /**
   * A política de timeout de uma resposta no cliente
   */
  private Policy timeOutPolicy;

  /**
   * Se conecta ao AccessControlServer por meio do endereço e da porta. Este
   * método também instancia um observador de <i>leases</i>.
   * 
   * @throws ACSUnavailableException Caso o serviço não seja encontrado.
   * @throws CORBAException Caso ocorra algum erro no acesso ao serviço.
   */
  public void fetchACS() throws ACSUnavailableException, CORBAException {
    this.ic = Utils.fetchAccessControlServiceComponent(orb, host, port);

    org.omg.CORBA.Object obj = ic.getFacet(IAccessControlServiceHelper.id());
    this.acs = IAccessControlServiceHelper.narrow(obj);

    obj = ic.getFacet(ILeaseProviderHelper.id());
    this.lp = ILeaseProviderHelper.narrow(obj);

    if (this.faultToleranceEnabled) {
      obj = ic.getFacet(IFaultTolerantServiceHelper.id());
      this.ft = IFaultTolerantServiceHelper.narrow(obj);

      this.acs._set_policy_override(new Policy[] { this.timeOutPolicy },
        SetOverrideType.ADD_OVERRIDE);
      this.lp._set_policy_override(new Policy[] { this.timeOutPolicy },
        SetOverrideType.ADD_OVERRIDE);
      this.ic._set_policy_override(new Policy[] { this.timeOutPolicy },
        SetOverrideType.ADD_OVERRIDE);
      this.ft._set_policy_override(new Policy[] { this.timeOutPolicy },
        SetOverrideType.ADD_OVERRIDE);
    }
  }

  /**
   * Construtor do barramento.
   */
  private Openbus() {
    resetInstance();
  }

  /**
   * Fornece a instância única do barramento.
   * 
   * @return A instância única do barramento.
   */
  public static Openbus getInstance() {
    if (instance == null) {
      instance = new Openbus();
    }
    return instance;
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * definições de atributos realizadas. Em seguida, inicializa o Orb.
   * 
   * @param args Conjunto de argumentos para a criação do ORB.
   * @param props Conjunto de propriedades para a criação do ORB.
   * @param host Endereço do Serviço de Controle de Acesso.
   * @param port Porta do Serviço de Controle de Acesso.
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws AlreadyInitializedException Caso a classe <i>Openbus</i> já tenha
   *         sido inicializada.
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public void init(String[] args, Properties props, String host, int port)
    throws UserException, AlreadyInitializedException {
    this
      .init(args, props, host, port, CredentialValidationPolicy.ALWAYS, false);
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * definições de atributos realizadas. Em seguida, inicializa o Orb.
   * 
   * @param args Conjunto de argumentos para a criação do ORB.
   * @param props Conjunto de propriedades para a criação do ORB.
   * @param host Endereço do Serviço de Controle de Acesso.
   * @param port Porta do Serviço de Controle de Acesso.
   * @param enableFaultTolerance Indica se o mecanismo de tolerância a falhas
   *        deve ser habilitado
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws AlreadyInitializedException Caso a classe <i>Openbus</i> já tenha
   *         sido inicializada.
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public void init(String[] args, Properties props, String host, int port,
    boolean enableFaultTolerance) throws UserException,
    AlreadyInitializedException {
    this.init(args, props, host, port, CredentialValidationPolicy.ALWAYS,
      enableFaultTolerance);
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * definições de atributos realizadas. Em seguida, inicializa o Orb.
   * 
   * @param args Conjunto de argumentos para a criação do ORB.
   * @param props Conjunto de propriedades para a criação do ORB.
   * @param host Endereço do Serviço de Controle de Acesso.
   * @param port Porta do Serviço de Controle de Acesso.
   * @param policy A política de validação de credenciais obtidas pelo
   *        interceptador servidor.
   * @param enableFaultTolerance Indica se o mecanismo de tolerância a falhas
   *        deve ser habilitado
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws AlreadyInitializedException Caso a classe <i>Openbus</i> já tenha
   *         sido inicializada.
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public void init(String[] args, Properties props, String host, int port,
    CredentialValidationPolicy policy, boolean enableFaultTolerance)
    throws UserException, AlreadyInitializedException {
    if (orb != null) {
      throw new AlreadyInitializedException(
        "O acesso ao barramento já está inicializado.");
    }

    if (host == null)
      throw new IllegalArgumentException("O campo 'host' não pode ser null");
    if (port < 0)
      throw new IllegalArgumentException(
        "O campo 'port' não pode ser negativo.");
    this.host = host;
    this.port = port;

    if (props == null)
      throw new IllegalArgumentException("O campo 'props' não pode ser null");
    String clientInitializerClassName = ClientInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + clientInitializerClassName,
      clientInitializerClassName);
    String serverInitializerClassName = ServerInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + serverInitializerClassName,
      serverInitializerClassName);

    // A política deve ser definida antes do ORB.init pois o
    // ServerInitializer
    // utiliza esta propriedade.
    this.credentialValidationPolicy = policy;
    this.faultToleranceEnabled = enableFaultTolerance;
    addInterceptedMethods();

    // TIMEOUT = 1000 * 36 * 5 = 180000 => 3 MINUTOS
    // TIMEOUT = 1000 * 12 * 1 = 12000 => 12 segundos
    // Esse timeout é considerado primeiro em relação ao definido pela política
    // e por isso está sendo configurado
    props.setProperty("jacorb.connection.client.connect_timeout", "1000");
    props.setProperty("jacorb.retries", "36");
    props.setProperty("jacorb.retry_interval", "5"); // tenta a cada 5
    // milesegundos
    props.setProperty("jacorb.poa.check_reply_end_time", "on");
    int totalTimeOut = 180000;

    // Set this policy's value in 100-nanosecond units
    timeOutPolicy = new RelativeRoundtripTimeoutPolicy(totalTimeOut * 10000);

    this.orb = org.omg.CORBA.ORB.init(args, props);

    org.omg.CORBA.Object obj = this.orb.resolve_initial_references("RootPOA");
    this.rootPOA = POAHelper.narrow(obj);
    POAManager manager = this.rootPOA.the_POAManager();
    manager.activate();
  }

  /**
   * Fornece o ORB.
   * 
   * @return O ORB.
   */
  public org.omg.CORBA.ORB getORB() {
    return this.orb;
  }

  /**
   * Obtém o RootPOA.
   * 
   * @return O RootPOA.
   */
  public POA getRootPOA() {
    return this.rootPOA;
  }

  /**
   * Executa o ORB.
   */
  public void run() {
    this.orb.run();
  }

  /**
   * Finaliza a execução do ORB.
   * 
   * @param force Se a finalização deve ser forçada ou não.
   */
  public void shutdown(boolean force) {
    this.orb.shutdown(!force);
  }

  /**
   * Fornece o Serviço de Controle de Acesso.
   * 
   * @return O Serviço de Controle de Acesso.
   */
  public IAccessControlService getAccessControlService() {
    return this.acs;
  }

  /**
   * Fornece o Serviço de Registro.
   * 
   * @return O Serviço de Registro.
   */
  public IRegistryService getRegistryService() {
    if (this.ic == null)
      return null;

    Logger logger = LoggerFactory.getLogger(Openbus.class);
    Object objRecep = this.ic.getFacetByName(Utils.RECEPTACLES_NAME);
    IReceptacles ireceptacle = IReceptaclesHelper.narrow(objRecep);

    IRegistryService registryService = null;
    try {
      ConnectionDescription[] connections =
        ireceptacle.getConnections(Utils.REGISTRY_SERVICE_RECEPTACLE_NAME);
      if (connections.length > 0) {
        Object objRef = connections[0].objref;
        IComponent registryComponent = IComponentHelper.narrow(objRef);
        Object objReg =
          registryComponent.getFacetByName(Utils.REGISTRY_SERVICE_FACET_NAME);
        registryService = IRegistryServiceHelper.narrow(objReg);

        if (this.faultToleranceEnabled) {
          registryService._set_policy_override(
            new Policy[] { this.timeOutPolicy }, SetOverrideType.ADD_OVERRIDE);
        }
      }
    }
    catch (InvalidName e) {
      logger.error("Não foi possível obter o serviço de registro.", e);
    }

    return registryService;
  }

  /**
   * Fornece a credencial da entidade.
   * 
   * @return A credencial.
   */
  public Credential getCredential() {
    Credential threadCredential = this.threadLocalCredential.get();
    if (threadCredential != null)
      return threadCredential;
    return this.credential;
  }

  /**
   * Define uma credencial a ser utilizada no lugar da credencial corrente. Útil
   * para fornecer uma credencial com o campo delegate preenchido.
   * 
   * @param credential Credencial a ser usada nas requisições a serem
   *        realizadas.
   */
  public void setThreadCredential(Credential credential) {
    this.threadLocalCredential.set(credential);
  }

  /**
   * Define o slot da credencial da requisição atual.
   * 
   * @param interceptedCredentialSlot O slot da credencial da requisição.
   */
  public void setInterceptedCredentialSlot(int interceptedCredentialSlot) {
    this.requestCredentialSlot = interceptedCredentialSlot;
  }

  /**
   * Fornece a credencial interceptada a partir da requisição atual.
   * 
   * @return A credencial da requisição.
   */
  public Credential getInterceptedCredential() {
    Logger logger = LoggerFactory.getLogger(Openbus.class);
    Current pic;
    try {
      pic =
        CurrentHelper.narrow(this.orb.resolve_initial_references("PICurrent"));
    }
    catch (org.omg.CORBA.ORBPackage.InvalidName e) {
      logger.error("Erro ao obter a credencial interceptada.", e);
      return null;
    }
    Any requestCredentialValue;
    try {
      requestCredentialValue = pic.get_slot(this.requestCredentialSlot);
    }
    catch (InvalidSlot e) {
      return null;
    }
    if (requestCredentialValue.type().kind().equals(TCKind.tk_null)) {
      return null;
    }
    return CredentialHelper.extract(requestCredentialValue);
  }

  /**
   * Realiza uma tentativa de conexão com o barramento (serviço de controle de
   * acesso e o serviço de registro), via nome de usuário e senha.
   * 
   * @param user Nome do usuário.
   * @param password Senha do usuário.
   * 
   * @return O serviço de registro.
   * 
   * @throws ACSLoginFailureException O par nome de usuário e senha não foram
   *         validados.
   * @throws ACSUnavailableException Caso o Serviço de Controle de Acesso não
   *         consiga ser contactado.
   * @throws ServiceUnavailableException
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Serviço de Registro.
   * @throws CORBAException
   * @throws OpenBusException
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public synchronized IRegistryService connect(String user, String password)
    throws ACSLoginFailureException, ACSUnavailableException,
    ServiceUnavailableException, InvalidCredentialException, CORBAException,
    OpenBusException {
    if ((user == null) || (password == null))
      throw new IllegalArgumentException(
        "Os parâmetros 'user' e 'password' não podem ser nulos.");
    Authenticator authenticator =
      new LoginPasswordAuthenticator(user, password);
    return this.connect(authenticator);
  }

  /**
   * Realiza uma tentativa de conexão com o barramento (serviço de controle de
   * acesso e o serviço de registro), via certificado.
   * 
   * @param name Nome do usuário.
   * @param privateKey Chave privada.
   * @param acsCertificate Certificado a ser fornecido ao Serviço de Controle de
   *        Acesso.
   * 
   * @return O Serviço de Registro.
   * 
   * @throws ACSLoginFailureException O certificado não foi validado.
   * @throws ServiceUnavailableException
   * @throws PKIException Os dados fornecidos não são válidos.
   * @throws ACSUnavailableException Caso o Serviço de Controle de Acesso não
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Serviço de Registro.
   * @throws OpenBusException
   * @throws CORBAException
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public synchronized IRegistryService connect(String name,
    RSAPrivateKey privateKey, X509Certificate acsCertificate)
    throws ACSLoginFailureException, ServiceUnavailableException, PKIException,
    ACSUnavailableException, InvalidCredentialException, OpenBusException,
    CORBAException {
    if ((name == null) || (privateKey == null) || (acsCertificate == null))
      throw new IllegalArgumentException("Nenhum parâmetro pode ser nulo.");
    Authenticator authenticator =
      new CertificateAuthenticator(name, privateKey, acsCertificate);
    return this.connect(authenticator);
  }

  /**
   * Realiza uma tentativa de conexão com o barramento (serviço de controle de
   * acesso e o serviço de registro), via certificado. A chave privada da
   * entidade e o certificado do barramento são carregados a partir de um
   * repositório {@link KeyStore}.
   * 
   * @param entityName O nome da entidade (system deployment) que deseja se
   *        conectar
   * @param keyStoreInputStream O repositório
   * @param keyStorePassword A senha do repositório.
   * @param entityKeyStoreAlias O apelido da entidade no repositório.
   * @param entityKeyStorePassword A senha da entidade no repositório.
   * @param openbusKeyStoreAlias O apelido do barramento no repositório.
   * 
   * @return O Serviço de Registro.
   * 
   * @throws OpenBusException
   * 
   * @see #connect(String, RSAPrivateKey, X509Certificate)
   */
  public IRegistryService connect(String entityName,
    InputStream keyStoreInputStream, char[] keyStorePassword,
    String entityKeyStoreAlias, char[] entityKeyStorePassword,
    String openbusKeyStoreAlias) throws OpenBusException {
    if (keyStoreInputStream == null) {
      throw new IllegalArgumentException("O repositório não pode ser nulo.");
    }

    Logger logger = LoggerFactory.getLogger(Openbus.class);
    KeyStore keyStore;
    try {
      keyStore = KeyStore.getInstance("JKS");
    }
    catch (GeneralSecurityException e) {
      throw new OpenBusException(e);
    }

    try {
      keyStore.load(keyStoreInputStream, keyStorePassword);
    }
    catch (IOException e) {
      throw new OpenBusException(e);
    }
    catch (GeneralSecurityException e) {
      throw new OpenBusException(e);
    }

    try {
      if (!keyStore.containsAlias(entityKeyStoreAlias)) {
        logger
          .error("O alias da entidade ({}) não existe", entityKeyStoreAlias);
        throw new OpenBusException("O alias da entidade não existe");
      }
      if (!keyStore.containsAlias(openbusKeyStoreAlias)) {
        logger.error("O alias do barramento ({}) não existe",
          openbusKeyStoreAlias);
        throw new OpenBusException("O alias do barramento não existe");
      }
    }
    catch (KeyStoreException e) {
      throw new OpenBusException(e);
    }

    RSAPrivateKey entityPrivateKey;
    try {
      entityPrivateKey =
        (RSAPrivateKey) keyStore.getKey(entityKeyStoreAlias,
          entityKeyStorePassword);
    }
    catch (GeneralSecurityException e) {
      throw new OpenBusException(e);
    }

    X509Certificate openBusCertificate;
    try {
      openBusCertificate =
        (X509Certificate) keyStore.getCertificate(openbusKeyStoreAlias);
    }
    catch (KeyStoreException e) {
      throw new OpenBusException(e);
    }

    return this.connect(entityName, entityPrivateKey, openBusCertificate);
  }

  /**
   * Realiza uma tentativa de conexão com o barramento (serviço de controle de
   * acesso e o serviço de registro).
   * 
   * @param authenticator O responsável por efetuar a autenticação.
   * 
   * @return O Serviço de Registro.
   * 
   * @throws ACSLoginFailureException O certificado não foi validado.
   * @throws OpenBusException
   */
  private IRegistryService connect(Authenticator authenticator)
    throws ACSLoginFailureException, OpenBusException {
    if (this.credential == null) {
      if (this.acs == null) {
        fetchACS();
      }
      this.credential = authenticator.authenticate(this.acs);
      if (this.credential != null) {
        this.leaseRenewer =
          new LeaseRenewer(this.credential, this.lp,
            new OpenbusExpiredCallback());
        this.leaseRenewer.start();
        return this.getRegistryService();
      }
      else {
        throw new ACSLoginFailureException(
          "Não foi possível conectar ao barramento.");
      }
    }
    else {
      throw new ACSLoginFailureException("O barramento já está conectado.");
    }
  }

  /**
   * <p>
   * Realiza uma tentativa de conexão com o barramento(serviço de controle de
   * acesso), a partir de uma credencial.
   * </p>
   * 
   * <p>
   * Este processo utiliza a conexão do dono da credencial. Logo ele não é
   * responsável por manter a conexão ativa - não renova o <i>lease</i>. Se o
   * processo dono da credencial se desconectar, este será desconectado.
   * </p>
   * 
   * @param credential A credencial.
   * 
   * @return O serviço de registro.
   * 
   * @throws ACSUnavailableException Caso o Serviço de Controle de Acesso não
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Serviço de Registro.
   * @throws OpenBusException
   * @throws ServiceUnavailableException
   * @throws CORBAException
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public IRegistryService connect(Credential credential)
    throws InvalidCredentialException, OpenBusException,
    ServiceUnavailableException, ACSUnavailableException, CORBAException {

    if (credential == null)
      throw new IllegalArgumentException(
        "O parâmetro 'credential' não pode ser nulo.");
    if (this.acs == null)
      fetchACS();
    this.credential = credential;
    if (this.acs.isValid(this.credential)) {
      return this.getRegistryService();
    }
    throw new InvalidCredentialException(new NO_PERMISSION(
      "Credencial inválida."));
  }

  /**
   * Desfaz a conexão atual.
   * 
   * @return {@code true} caso a conexão seja desfeita, ou {@code false} se
   *         nenhuma conexão estiver ativa.
   */
  public synchronized boolean disconnect() {
    if (this.credential == null)
      return false;

    try {
      if (this.leaseRenewer != null) {
        this.leaseRenewer.stop();
        this.acs.logout(this.credential);
      }
    }
    finally {
      reset();
    }
    return true;
  }

  /**
   * Finaliza a utilização do barramento.
   */
  public void destroy() {
    this.resetInstance();
  }

  /**
   * Finaliza a utilização do barramento.
   */
  public void resetInstance() {
    if (this.orb != null) {
      this.orb.destroy();
    }
    this.orb = null;
    this.rootPOA = null;
    this.host = null;
    this.port = INVALID_PORT;
    this.leaseExpiredCallback = null;
    this.ifaceMap = new HashMap<String, Set<String>>();
    this.faultToleranceEnabled = false;

    this.reset();
  }

  /**
   * Retorna para o estado encontrado após a inicialização do Openbus.
   */
  private void reset() {
    this.acs = null;
    this.lp = null;
    this.ft = null;
    this.ic = null;

    this.credential = null;
    this.threadLocalCredential = new ThreadLocal<Credential>();
    this.leaseRenewer = null;
    this.requestCredentialSlot = INVALID_CREDENTIAL_SLOT;
  }

  /**
   * Informa o estado de conexão com o barramento.
   * 
   * @return {@code true} caso a conexão esteja ativa, ou {@code false}, caso
   *         contrário.
   */
  public boolean isConnected() {
    return (this.credential != null);
  }

  /**
   * Atribui o observador para receber eventos de expiração do <i>lease</i>.
   * 
   * @param lec O observador.
   * 
   */
  public void setLeaseExpiredCallback(LeaseExpiredCallback lec) {
    this.leaseExpiredCallback = lec;
  }

  /**
   * Remove o observador de expiração do <i>lease</i>.
   */
  public void removeLeaseExpiredCallback() {
    this.leaseExpiredCallback = null;
  }

  /**
   * Controla se o método deve ou não ser interceptado pelo servidor.
   * 
   * @param iface RepID da interface.
   * @param method Nome do método.
   * @param interceptable Indica se o método deve ser inteceptado ou não.
   * 
   */
  public void setInterceptable(String iface, String method,
    boolean interceptable) {
    Set<String> methods = ifaceMap.get(iface);
    if (interceptable) {
      if (methods != null) {
        methods.remove(method);
        if (methods.size() == 0)
          ifaceMap.remove(iface);
      }
    }
    else {
      if (methods == null) {
        methods = new HashSet<String>();
        ifaceMap.put(iface, methods);
      }
      methods.add(method);
    }
  }

  /**
   * Indica se o método da interface deve ser interceptado. Irá verificar se o
   * método se encontra no <i>iface</i> ou em super interfaces de <i>iface</i>.
   * 
   * @param iface RepID da interface.
   * @param method Nome do método a ser testado.
   * 
   * @return <code>true</code> se o método de ver interceptado, caso contrário
   *         <code>false</code>.
   */
  public boolean isInterceptable(String iface, String method) {
    List<Set<String>> methodsList = new ArrayList<Set<String>>();
    Set<String> methods = ifaceMap.get(iface);
    if (methods != null)
      methodsList.add(methods);

    // TODO: Verificar se é necessário procurar o "method" em todas as
    // superinterfaces de "iface".
    String corbaObjRepID = "org.omg.CORBA.Object";
    methods = ifaceMap.get(corbaObjRepID);
    if (methods != null)
      methodsList.add(methods);

    for (Set<String> methodSet : methodsList)
      if (methodSet.contains(method))
        return false;

    return true;
  }

  /**
   * Indica se o mecanismo de tolerancia a falhas esta ativado.
   * 
   * @return <code>true</code> se o mecanismo está ativo e <code>false</code>,
   *         caso contrário.
   * 
   */
  public boolean isFaultToleranceEnabled() {
    return faultToleranceEnabled;
  }

  /**
   * Define o endereço do Serviço de Controle de Acesso.
   * 
   * @param host Endereço do Serviço de Controle de Acesso.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Define a porta do Serviço de Controle de Acesso.
   * 
   * @param hostPort A porta do Serviço de Controle de Acesso.
   */
  public void setPort(int hostPort) {
    this.port = hostPort;
  }

  /**
   * Fornece o Provedor de Lease do Serviço de Controle de Acesso.
   * 
   * @return O Provedor de Lease.
   */
  public ILeaseProvider getLeaseProvider() {
    return lp;
  }

  /**
   * Fornece o Serviço de Tolerância a Falhas do Serviço de Controle de Acesso.
   * 
   * @return O Serviço de Tolerância a Falhas.
   */
  public IFaultTolerantService getACSFaultTolerantService() {
    return ft;
  }

  /**
   * Fornece a faceta IComponent do Serviço de Controle de Acesso.
   * 
   * @return O IComponent.
   */
  public IComponent getACSIComponent() {
    return ic;
  }

  /**
   * Obtém a política de validação de credenciais obtidas pelo interceptador
   * servidor.
   * 
   * @return A política de validação.
   */
  public CredentialValidationPolicy getCredentialValidationPolicy() {
    return this.credentialValidationPolicy;
  }

  /**
   * Adiciona os métodos internos que devem ser interceptados na API.
   */
  private void addInterceptedMethods() {
    /*
     * Work around para o LocateRequest
     * 
     * Durante o bind com o servidor, o cliente Orbix envia uma mensagem GIOP
     * 1.2 LocateRequest para o servidor, que é uma otimização corba para
     * localizar o objeto. Esta mensageme não passa pelo nosso interceptador
     * cliente e portanto a mensagem é envidada sem a credencial. O JacORB sabe
     * lidar com essa mensagen GIOP, porém diferentemente do Orbix, ele passa
     * essa mensagem pelo interceptador do servidor, que por sua vez faz uma
     * verificação que falha por falta de credencial. Essa mensagem não deve ser
     * verificada.
     * 
     * Analisando o código do JacORB, podemos ver que para uso interno, ele
     * define esse request como uma operação de nome "_non_existent". Então no
     * interceptador do servidor JacORB nós podemos ver esse request com a
     * operação com esse nome.
     * 
     * Logo para podermos responder adequadamente com um GIOP 1.2 LocateReply,
     * foi adicionado uma condição que inibe a verificação no caso de ser essa
     * operação interceptada.
     */
    setInterceptable("org.omg.CORBA.Object", "_non_existent", false);
  }

  /**
   * Informa aos observadores que o <i>lease</i> expirou.
   */
  private void leaseExpired() {
    Logger logger = LoggerFactory.getLogger(Openbus.class);
    logger.debug("Atualizando estado do Openbus");
    reset();
    if (this.leaseExpiredCallback != null) {
      this.leaseExpiredCallback.expired();
    }
  }

  /**
   * Classe responsável por informar aos observadores que o <i>lease</i>
   * expirou.
   * 
   * @author Tecgraf
   */
  class OpenbusExpiredCallback implements LeaseExpiredCallback {
    public void expired() {
      leaseExpired();
    }
  }

}
