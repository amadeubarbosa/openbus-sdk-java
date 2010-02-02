/*
 * $Id$
 */
package tecgraf.openbus;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.omg.CORBA.Any;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;

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
import tecgraf.openbus.core.v1_05.access_control_service.ILeaseProvider;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryServiceHelper;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;
import tecgraf.openbus.exception.InvalidCredentialException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.PKIException;
import tecgraf.openbus.exception.ServiceUnavailableException;
import tecgraf.openbus.fault_tolerance.v1_05.IFaultTolerantService;
import tecgraf.openbus.interceptors.ClientInitializer;
import tecgraf.openbus.interceptors.CredentialValidationPolicy;
import tecgraf.openbus.interceptors.FTClientInitializer;
import tecgraf.openbus.interceptors.ServerInitializer;
import tecgraf.openbus.lease.LeaseExpiredCallback;
import tecgraf.openbus.lease.LeaseRenewer;
import tecgraf.openbus.session_service.v1_05.ISessionService;
import tecgraf.openbus.session_service.v1_05.ISessionServiceHelper;
import tecgraf.openbus.util.Log;
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
   * Serviço de registro.
   */
  private IRegistryService rgs;
  /**
   * Serviço de sessão.
   */
  private ISessionService ss;
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
  private boolean isFaultToleranceEnable;

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
   * Se conecta ao AccessControlServer por meio do endereço e da porta. Este
   * método também instancia um observador de <i>leases</i>.
   * 
   * @throws CORBAException
   * @throws ServiceUnavailableException
   * @throws ACSUnavailableException
   */
  public void fetchACS() throws CORBAException, ServiceUnavailableException,
    ACSUnavailableException {
    this.acs = Utils.fetchAccessControlService(orb, host, port);
    this.lp = Utils.fetchAccessControlServiceLeaseProvider(orb, host, port);
    this.ic = Utils.fetchAccessControlServiceIComponent(orb, host, port);
    if (this.isFaultToleranceEnable) {
      this.ft = Utils.fetchAccessControlServiceFaultTolerant(orb, host, port);
    }
  }

  /**
   * Construtor do barramento.
   */
  private Openbus() {
    this.port = INVALID_PORT;
    this.requestCredentialSlot = INVALID_CREDENTIAL_SLOT;
    this.ifaceMap = new HashMap<String, Set<String>>();
    this.threadLocalCredential = new ThreadLocal<Credential>();
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
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public void init(String[] args, Properties props, String host, int port)
    throws UserException {
    this.init(args, props, host, port, CredentialValidationPolicy.ALWAYS);
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
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public void init(String[] args, Properties props, String host, int port,
    CredentialValidationPolicy policy) throws UserException {
    if (orb != null) {
      return;
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

    this.orb = org.omg.CORBA.ORB.init(args, props);

    org.omg.CORBA.Object obj = this.orb.resolve_initial_references("RootPOA");
    this.rootPOA = POAHelper.narrow(obj);
    POAManager manager = this.rootPOA.the_POAManager();
    manager.activate();
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * definições de atributos realizadas. Em seguida, inicializa o Orb com
   * mecanismo de tolerancia a falhas ativado
   * 
   * @param args Conjunto de argumentos para a criação do ORB.
   * @param props Conjunto de propriedades para a criação do ORB.
   * @param host Endereço do Serviço de Controle de Acesso.
   * @param port Porta do Serviço de Controle de Acesso.
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public void initWithFaultTolerance(String[] args, Properties props,
    String host, int port) throws UserException {
    if (orb != null) {
      return;
    }

    if (host == null)
      throw new IllegalArgumentException("O campo 'host' não pode ser null");
    if (port < 0)
      throw new IllegalArgumentException(
        "O campo 'port' não pode ser negativo.");
    this.host = host;
    this.port = port;

    this.isFaultToleranceEnable = true;

    String clientInitializerClassName = FTClientInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + clientInitializerClassName,
      clientInitializerClassName);
    //TODO mudar para FTServerInitializer
    String serverInitializerClassName = ServerInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + serverInitializerClassName,
      serverInitializerClassName);
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
    if (this.rgs != null)
      return this.rgs;
    if (this.ic == null)
      return null;

    Object objRecep = this.ic.getFacetByName(Utils.RECEPTACLES_NAME);
    IReceptacles ireceptacle = IReceptaclesHelper.narrow(objRecep);

    try {
      ConnectionDescription[] connections =
        ireceptacle.getConnections(Utils.REGISTRY_SERVICE_RECEPTACLE_NAME);
      if (connections.length > 0) {
        Object objRef = connections[0].objref;
        IComponent registryComponent = IComponentHelper.narrow(objRef);
        Object objReg =
          registryComponent.getFacetByName(Utils.REGISTRY_SERVICE_FACET_NAME);
        this.rgs = IRegistryServiceHelper.narrow(objReg);
      }
    }
    catch (InvalidName e) {
      Log.COMMON.severe("Não foi possível obter o serviço de registro.", e);
    }

    return this.rgs;
  }

  /**
   * Fornece o Serviço de Sessão. Caso o Openbus ainda não tenha a referência
   * para o Serviço de Sessão este obtem o Serviço a partir do Serviço de
   * Registro.
   * 
   * @return O Serviço de Sessão.
   */
  public ISessionService getSessionService() {

    if (this.ss == null) {
      IRegistryService rgs = this.getRegistryService();
      if (rgs == null)
        return null;
      ServiceOffer[] offers =
        rgs.find(new String[] { Utils.SESSION_SERVICE_FACET_NAME });
      if (offers.length > 0) {
        IComponent component = offers[0].member;
        Object facet = component.getFacet(ISessionServiceHelper.id());
        if (facet == null) {
          return null;
        }
        this.ss = ISessionServiceHelper.narrow(facet);
        return this.ss;
      }
      return null;
    }
    return this.ss;
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
    try {
      Current pic =
        CurrentHelper.narrow(this.orb.resolve_initial_references("PICurrent"));
      Any requestCredentialValue = pic.get_slot(this.requestCredentialSlot);
      if (requestCredentialValue.type().kind().equals(TCKind.tk_null)) {
        return null;
      }
      Credential requestCredential =
        CredentialHelper.extract(requestCredentialValue);
      return requestCredential;
    }
    catch (org.omg.CORBA.UserException e) {
      Log.COMMON.severe("Erro ao obter a credencial da requisição,", e);
      return null;
    }
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
          new LeaseRenewer(this.credential, this.lp, this.leaseExpiredCallback);
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
   * Realiza uma tentativa de conexão com o barramento(serviço de controle de
   * acesso e o serviço de registro), a partir de uma credencial.
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
    if (this.credential != null) {
      try {
        this.leaseRenewer.stop();
        this.acs.logout(this.credential);
      }
      finally {
        this.leaseRenewer = null;
        this.credential = null;
      }
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Finaliza a utilização do barramento.
   */
  public void destroy() {
    if (this.orb == null) {
      return;
    }

    this.orb.destroy();
    this.orb = null;
    this.rootPOA = null;
    this.host = null;
    this.port = INVALID_PORT;
    this.acs = null;
    this.rgs = null;
    this.lp = null;
    this.ft = null;
    this.ic = null;
    this.ss = null;
    this.credential = null;
    this.leaseRenewer = null;
    this.leaseExpiredCallback = null;
    this.isFaultToleranceEnable = false;

    this.threadLocalCredential.set(null);
    this.ifaceMap = new HashMap<String, Set<String>>();
    this.requestCredentialSlot = INVALID_CREDENTIAL_SLOT;
    this.port = INVALID_PORT;
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
  public void addLeaseExpiredCallback(LeaseExpiredCallback lec) {
    this.leaseExpiredCallback = lec;
    if (this.leaseRenewer != null) {
      this.leaseRenewer.setLeaseExpiredCallback(lec);
    }
  }

  /**
   * Remove o observador de expiração do <i>lease</i>.
   */
  public void removeLeaseExpiredCallback() {
    this.leaseExpiredCallback = null;
    if (this.leaseRenewer != null) {
      this.leaseRenewer.setLeaseExpiredCallback(null);
    }
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
    Set<String> methods;
    if (interceptable) {
      methods = ifaceMap.get(iface);
      if (methods != null) {
        methods.remove(method);
        if (methods.size() == 0)
          ifaceMap.remove(iface);
      }
    }
    else {
      methods = ifaceMap.get(iface);
      if (methods == null) {
        methods = new HashSet<String>();
        ifaceMap.put(iface, methods);
      }
      methods.add(method);
    }
  }

  /**
   * Indica se o método da interface dever interceptado.
   * 
   * @param iface RepID da interface.
   * @param method Nome do método a ser testado.
   * 
   * @return <code>true</code> se o método de ver interceptado, caso contrário
   *         <code>false</code>.
   */
  public boolean isInterceptable(String iface, String method) {
    Set<String> methods = ifaceMap.get(iface);
    return (methods == null) || !methods.contains(method);
  }

  /**
   * Indica se o mecanismo de tolerancia a falhas esta ativado.
   * 
   * @return <code>true</code> se o mecanismo está ativo e <code>false</code>,
   *         caso contrário.
   * 
   */
  public boolean isFaultToleranceEnable() {
    return isFaultToleranceEnable;
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
   * Obtém a política de validação de credenciais obtidas pelo interceptador
   * servidor.
   * 
   * @return A política de validação.
   */
  public CredentialValidationPolicy getCredentialValidationPolicy() {
    return this.credentialValidationPolicy;
  }
}
