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
   * Um valor inv�lido para a porta do Servi�o de Controle de Acesso.
   */
  private static final int INVALID_PORT = -1;
  /**
   * Um valor inv�lido para o slot da credencial.
   */
  private static final int INVALID_CREDENTIAL_SLOT = -1;
  /**
   * A inst�ncia �nica do barramento.
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
   * O host do Servi�o de Controle de Acesso.
   */
  private String host;
  /**
   * A porta do Servi�o de Controle de Acesso.
   */
  private int port;
  /**
   * O Servi�o de Controle de Acesso.
   */
  private IAccessControlService acs;
  /**
   * Interface ILeaseProvider do Servi�o de Controle de Acesso.
   */
  private ILeaseProvider lp;

  /**
   * Interface IFaultTolerantService do Servi�o de Controle de Acesso.
   */
  private IFaultTolerantService ft;
  /**
   * 
   * Interface ILeaseProvider do Servi�o de Controle de Acesso.
   */
  private IComponent ic;
  /**
   * O renovador do <i>lease</i>.
   */
  private LeaseRenewer leaseRenewer;
  /**
   * <i>Callback</i> para a notifica��o de que um <i>lease</i> expirou.
   */
  private LeaseExpiredCallback leaseExpiredCallback;
  /**
   * Credencial recebida ao se conectar ao barramento.
   */
  private Credential credential;
  /**
   * A credencial da entidade, v�lida apenas na <i>thread</i> corrente.
   */
  private ThreadLocal<Credential> threadLocalCredential;
  /**
   * O slot da credencial da requisi��o.
   */
  private int requestCredentialSlot;
  /**
   * Indica se o mecanismo de tolerancia a falhas esta ativado.
   */
  private boolean faultToleranceEnabled;

  /**
   * Mant�m a lista de m�todos a serem liberados por interface.
   */
  private Map<String, Set<String>> ifaceMap;

  /**
   * A pol�tica de valida��o das credenciais obtidas pelo interceptador
   * servidor.
   */
  private CredentialValidationPolicy credentialValidationPolicy;

  /**
   * A pol�tica de timeout de uma resposta no cliente
   */
  private Policy timeOutPolicy;

  /**
   * Se conecta ao AccessControlServer por meio do endere�o e da porta. Este
   * m�todo tamb�m instancia um observador de <i>leases</i>.
   * 
   * @throws ACSUnavailableException Caso o servi�o n�o seja encontrado.
   * @throws CORBAException Caso ocorra algum erro no acesso ao servi�o.
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
   * Fornece a inst�ncia �nica do barramento.
   * 
   * @return A inst�ncia �nica do barramento.
   */
  public static Openbus getInstance() {
    if (instance == null) {
      instance = new Openbus();
    }
    return instance;
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * defini��es de atributos realizadas. Em seguida, inicializa o Orb.
   * 
   * @param args Conjunto de argumentos para a cria��o do ORB.
   * @param props Conjunto de propriedades para a cria��o do ORB.
   * @param host Endere�o do Servi�o de Controle de Acesso.
   * @param port Porta do Servi�o de Controle de Acesso.
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws AlreadyInitializedException Caso a classe <i>Openbus</i> j� tenha
   *         sido inicializada.
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public void init(String[] args, Properties props, String host, int port)
    throws UserException, AlreadyInitializedException {
    this
      .init(args, props, host, port, CredentialValidationPolicy.ALWAYS, false);
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * defini��es de atributos realizadas. Em seguida, inicializa o Orb.
   * 
   * @param args Conjunto de argumentos para a cria��o do ORB.
   * @param props Conjunto de propriedades para a cria��o do ORB.
   * @param host Endere�o do Servi�o de Controle de Acesso.
   * @param port Porta do Servi�o de Controle de Acesso.
   * @param enableFaultTolerance Indica se o mecanismo de toler�ncia a falhas
   *        deve ser habilitado
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws AlreadyInitializedException Caso a classe <i>Openbus</i> j� tenha
   *         sido inicializada.
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
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
   * defini��es de atributos realizadas. Em seguida, inicializa o Orb.
   * 
   * @param args Conjunto de argumentos para a cria��o do ORB.
   * @param props Conjunto de propriedades para a cria��o do ORB.
   * @param host Endere�o do Servi�o de Controle de Acesso.
   * @param port Porta do Servi�o de Controle de Acesso.
   * @param policy A pol�tica de valida��o de credenciais obtidas pelo
   *        interceptador servidor.
   * @param enableFaultTolerance Indica se o mecanismo de toler�ncia a falhas
   *        deve ser habilitado
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws AlreadyInitializedException Caso a classe <i>Openbus</i> j� tenha
   *         sido inicializada.
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public void init(String[] args, Properties props, String host, int port,
    CredentialValidationPolicy policy, boolean enableFaultTolerance)
    throws UserException, AlreadyInitializedException {
    if (orb != null) {
      throw new AlreadyInitializedException(
        "O acesso ao barramento j� est� inicializado.");
    }

    if (host == null)
      throw new IllegalArgumentException("O campo 'host' n�o pode ser null");
    if (port < 0)
      throw new IllegalArgumentException(
        "O campo 'port' n�o pode ser negativo.");
    this.host = host;
    this.port = port;

    if (props == null)
      throw new IllegalArgumentException("O campo 'props' n�o pode ser null");
    String clientInitializerClassName = ClientInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + clientInitializerClassName,
      clientInitializerClassName);
    String serverInitializerClassName = ServerInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + serverInitializerClassName,
      serverInitializerClassName);

    // A pol�tica deve ser definida antes do ORB.init pois o
    // ServerInitializer
    // utiliza esta propriedade.
    this.credentialValidationPolicy = policy;
    this.faultToleranceEnabled = enableFaultTolerance;
    addInterceptedMethods();

    // TIMEOUT = 1000 * 36 * 5 = 180000 => 3 MINUTOS
    // TIMEOUT = 1000 * 12 * 1 = 12000 => 12 segundos
    // Esse timeout � considerado primeiro em rela��o ao definido pela pol�tica
    // e por isso est� sendo configurado
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
   * Obt�m o RootPOA.
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
   * Finaliza a execu��o do ORB.
   * 
   * @param force Se a finaliza��o deve ser for�ada ou n�o.
   */
  public void shutdown(boolean force) {
    this.orb.shutdown(!force);
  }

  /**
   * Fornece o Servi�o de Controle de Acesso.
   * 
   * @return O Servi�o de Controle de Acesso.
   */
  public IAccessControlService getAccessControlService() {
    return this.acs;
  }

  /**
   * Fornece o Servi�o de Registro.
   * 
   * @return O Servi�o de Registro.
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
      logger.error("N�o foi poss�vel obter o servi�o de registro.", e);
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
   * Define uma credencial a ser utilizada no lugar da credencial corrente. �til
   * para fornecer uma credencial com o campo delegate preenchido.
   * 
   * @param credential Credencial a ser usada nas requisi��es a serem
   *        realizadas.
   */
  public void setThreadCredential(Credential credential) {
    this.threadLocalCredential.set(credential);
  }

  /**
   * Define o slot da credencial da requisi��o atual.
   * 
   * @param interceptedCredentialSlot O slot da credencial da requisi��o.
   */
  public void setInterceptedCredentialSlot(int interceptedCredentialSlot) {
    this.requestCredentialSlot = interceptedCredentialSlot;
  }

  /**
   * Fornece a credencial interceptada a partir da requisi��o atual.
   * 
   * @return A credencial da requisi��o.
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
   * Realiza uma tentativa de conex�o com o barramento (servi�o de controle de
   * acesso e o servi�o de registro), via nome de usu�rio e senha.
   * 
   * @param user Nome do usu�rio.
   * @param password Senha do usu�rio.
   * 
   * @return O servi�o de registro.
   * 
   * @throws ACSLoginFailureException O par nome de usu�rio e senha n�o foram
   *         validados.
   * @throws ACSUnavailableException Caso o Servi�o de Controle de Acesso n�o
   *         consiga ser contactado.
   * @throws ServiceUnavailableException
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Servi�o de Registro.
   * @throws CORBAException
   * @throws OpenBusException
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public synchronized IRegistryService connect(String user, String password)
    throws ACSLoginFailureException, ACSUnavailableException,
    ServiceUnavailableException, InvalidCredentialException, CORBAException,
    OpenBusException {
    if ((user == null) || (password == null))
      throw new IllegalArgumentException(
        "Os par�metros 'user' e 'password' n�o podem ser nulos.");
    Authenticator authenticator =
      new LoginPasswordAuthenticator(user, password);
    return this.connect(authenticator);
  }

  /**
   * Realiza uma tentativa de conex�o com o barramento (servi�o de controle de
   * acesso e o servi�o de registro), via certificado.
   * 
   * @param name Nome do usu�rio.
   * @param privateKey Chave privada.
   * @param acsCertificate Certificado a ser fornecido ao Servi�o de Controle de
   *        Acesso.
   * 
   * @return O Servi�o de Registro.
   * 
   * @throws ACSLoginFailureException O certificado n�o foi validado.
   * @throws ServiceUnavailableException
   * @throws PKIException Os dados fornecidos n�o s�o v�lidos.
   * @throws ACSUnavailableException Caso o Servi�o de Controle de Acesso n�o
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Servi�o de Registro.
   * @throws OpenBusException
   * @throws CORBAException
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public synchronized IRegistryService connect(String name,
    RSAPrivateKey privateKey, X509Certificate acsCertificate)
    throws ACSLoginFailureException, ServiceUnavailableException, PKIException,
    ACSUnavailableException, InvalidCredentialException, OpenBusException,
    CORBAException {
    if ((name == null) || (privateKey == null) || (acsCertificate == null))
      throw new IllegalArgumentException("Nenhum par�metro pode ser nulo.");
    Authenticator authenticator =
      new CertificateAuthenticator(name, privateKey, acsCertificate);
    return this.connect(authenticator);
  }

  /**
   * Realiza uma tentativa de conex�o com o barramento (servi�o de controle de
   * acesso e o servi�o de registro), via certificado. A chave privada da
   * entidade e o certificado do barramento s�o carregados a partir de um
   * reposit�rio {@link KeyStore}.
   * 
   * @param entityName O nome da entidade (system deployment) que deseja se
   *        conectar
   * @param keyStoreInputStream O reposit�rio
   * @param keyStorePassword A senha do reposit�rio.
   * @param entityKeyStoreAlias O apelido da entidade no reposit�rio.
   * @param entityKeyStorePassword A senha da entidade no reposit�rio.
   * @param openbusKeyStoreAlias O apelido do barramento no reposit�rio.
   * 
   * @return O Servi�o de Registro.
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
      throw new IllegalArgumentException("O reposit�rio n�o pode ser nulo.");
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
          .error("O alias da entidade ({}) n�o existe", entityKeyStoreAlias);
        throw new OpenBusException("O alias da entidade n�o existe");
      }
      if (!keyStore.containsAlias(openbusKeyStoreAlias)) {
        logger.error("O alias do barramento ({}) n�o existe",
          openbusKeyStoreAlias);
        throw new OpenBusException("O alias do barramento n�o existe");
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
   * Realiza uma tentativa de conex�o com o barramento (servi�o de controle de
   * acesso e o servi�o de registro).
   * 
   * @param authenticator O respons�vel por efetuar a autentica��o.
   * 
   * @return O Servi�o de Registro.
   * 
   * @throws ACSLoginFailureException O certificado n�o foi validado.
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
          "N�o foi poss�vel conectar ao barramento.");
      }
    }
    else {
      throw new ACSLoginFailureException("O barramento j� est� conectado.");
    }
  }

  /**
   * <p>
   * Realiza uma tentativa de conex�o com o barramento(servi�o de controle de
   * acesso), a partir de uma credencial.
   * </p>
   * 
   * <p>
   * Este processo utiliza a conex�o do dono da credencial. Logo ele n�o �
   * respons�vel por manter a conex�o ativa - n�o renova o <i>lease</i>. Se o
   * processo dono da credencial se desconectar, este ser� desconectado.
   * </p>
   * 
   * @param credential A credencial.
   * 
   * @return O servi�o de registro.
   * 
   * @throws ACSUnavailableException Caso o Servi�o de Controle de Acesso n�o
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Servi�o de Registro.
   * @throws OpenBusException
   * @throws ServiceUnavailableException
   * @throws CORBAException
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public IRegistryService connect(Credential credential)
    throws InvalidCredentialException, OpenBusException,
    ServiceUnavailableException, ACSUnavailableException, CORBAException {

    if (credential == null)
      throw new IllegalArgumentException(
        "O par�metro 'credential' n�o pode ser nulo.");
    if (this.acs == null)
      fetchACS();
    this.credential = credential;
    if (this.acs.isValid(this.credential)) {
      return this.getRegistryService();
    }
    throw new InvalidCredentialException(new NO_PERMISSION(
      "Credencial inv�lida."));
  }

  /**
   * Desfaz a conex�o atual.
   * 
   * @return {@code true} caso a conex�o seja desfeita, ou {@code false} se
   *         nenhuma conex�o estiver ativa.
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
   * Finaliza a utiliza��o do barramento.
   */
  public void destroy() {
    this.resetInstance();
  }

  /**
   * Finaliza a utiliza��o do barramento.
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
   * Retorna para o estado encontrado ap�s a inicializa��o do Openbus.
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
   * Informa o estado de conex�o com o barramento.
   * 
   * @return {@code true} caso a conex�o esteja ativa, ou {@code false}, caso
   *         contr�rio.
   */
  public boolean isConnected() {
    return (this.credential != null);
  }

  /**
   * Atribui o observador para receber eventos de expira��o do <i>lease</i>.
   * 
   * @param lec O observador.
   * 
   */
  public void setLeaseExpiredCallback(LeaseExpiredCallback lec) {
    this.leaseExpiredCallback = lec;
  }

  /**
   * Remove o observador de expira��o do <i>lease</i>.
   */
  public void removeLeaseExpiredCallback() {
    this.leaseExpiredCallback = null;
  }

  /**
   * Controla se o m�todo deve ou n�o ser interceptado pelo servidor.
   * 
   * @param iface RepID da interface.
   * @param method Nome do m�todo.
   * @param interceptable Indica se o m�todo deve ser inteceptado ou n�o.
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
   * Indica se o m�todo da interface deve ser interceptado. Ir� verificar se o
   * m�todo se encontra no <i>iface</i> ou em super interfaces de <i>iface</i>.
   * 
   * @param iface RepID da interface.
   * @param method Nome do m�todo a ser testado.
   * 
   * @return <code>true</code> se o m�todo de ver interceptado, caso contr�rio
   *         <code>false</code>.
   */
  public boolean isInterceptable(String iface, String method) {
    List<Set<String>> methodsList = new ArrayList<Set<String>>();
    Set<String> methods = ifaceMap.get(iface);
    if (methods != null)
      methodsList.add(methods);

    // TODO: Verificar se � necess�rio procurar o "method" em todas as
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
   * @return <code>true</code> se o mecanismo est� ativo e <code>false</code>,
   *         caso contr�rio.
   * 
   */
  public boolean isFaultToleranceEnabled() {
    return faultToleranceEnabled;
  }

  /**
   * Define o endere�o do Servi�o de Controle de Acesso.
   * 
   * @param host Endere�o do Servi�o de Controle de Acesso.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Define a porta do Servi�o de Controle de Acesso.
   * 
   * @param hostPort A porta do Servi�o de Controle de Acesso.
   */
  public void setPort(int hostPort) {
    this.port = hostPort;
  }

  /**
   * Fornece o Provedor de Lease do Servi�o de Controle de Acesso.
   * 
   * @return O Provedor de Lease.
   */
  public ILeaseProvider getLeaseProvider() {
    return lp;
  }

  /**
   * Fornece o Servi�o de Toler�ncia a Falhas do Servi�o de Controle de Acesso.
   * 
   * @return O Servi�o de Toler�ncia a Falhas.
   */
  public IFaultTolerantService getACSFaultTolerantService() {
    return ft;
  }

  /**
   * Fornece a faceta IComponent do Servi�o de Controle de Acesso.
   * 
   * @return O IComponent.
   */
  public IComponent getACSIComponent() {
    return ic;
  }

  /**
   * Obt�m a pol�tica de valida��o de credenciais obtidas pelo interceptador
   * servidor.
   * 
   * @return A pol�tica de valida��o.
   */
  public CredentialValidationPolicy getCredentialValidationPolicy() {
    return this.credentialValidationPolicy;
  }

  /**
   * Adiciona os m�todos internos que devem ser interceptados na API.
   */
  private void addInterceptedMethods() {
    /*
     * Work around para o LocateRequest
     * 
     * Durante o bind com o servidor, o cliente Orbix envia uma mensagem GIOP
     * 1.2 LocateRequest para o servidor, que � uma otimiza��o corba para
     * localizar o objeto. Esta mensageme n�o passa pelo nosso interceptador
     * cliente e portanto a mensagem � envidada sem a credencial. O JacORB sabe
     * lidar com essa mensagen GIOP, por�m diferentemente do Orbix, ele passa
     * essa mensagem pelo interceptador do servidor, que por sua vez faz uma
     * verifica��o que falha por falta de credencial. Essa mensagem n�o deve ser
     * verificada.
     * 
     * Analisando o c�digo do JacORB, podemos ver que para uso interno, ele
     * define esse request como uma opera��o de nome "_non_existent". Ent�o no
     * interceptador do servidor JacORB n�s podemos ver esse request com a
     * opera��o com esse nome.
     * 
     * Logo para podermos responder adequadamente com um GIOP 1.2 LocateReply,
     * foi adicionado uma condi��o que inibe a verifica��o no caso de ser essa
     * opera��o interceptada.
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
   * Classe respons�vel por informar aos observadores que o <i>lease</i>
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
