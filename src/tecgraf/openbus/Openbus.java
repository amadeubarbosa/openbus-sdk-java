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

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;
import openbusidl.acs.IAccessControlService;
import openbusidl.acs.ILeaseProvider;
import openbusidl.ft.IFaultTolerantService;
import openbusidl.rs.IRegistryService;
import openbusidl.rs.ServiceOffer;
import openbusidl.ss.ISessionService;
import openbusidl.ss.ISessionServiceHelper;

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

import scs.core.IComponent;
import tecgraf.openbus.authenticators.Authenticator;
import tecgraf.openbus.authenticators.CertificateAuthenticator;
import tecgraf.openbus.authenticators.LoginPasswordAuthenticator;
import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;
import tecgraf.openbus.exception.InvalidCredentialException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.PKIException;
import tecgraf.openbus.exception.ServiceUnavailableException;
import tecgraf.openbus.interceptors.ClientInitializer;
import tecgraf.openbus.interceptors.FTClientInitializer;
import tecgraf.openbus.interceptors.ServerInitializer;
import tecgraf.openbus.lease.LeaseExpiredCallback;
import tecgraf.openbus.lease.LeaseRenewer;
import tecgraf.openbus.util.Log;
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
   * Servi�o de sess�o.
   */
  private ISessionService ss;
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
  private boolean isFaultToleranceEnable;

  /**
   * Mant�m a lista de m�todos a serem liberados por interface.
   */
  private Map<String, Set<String>> ifaceMap;

  /**
   * Se conecta ao AccessControlServer por meio do endere�o e da porta. Este
   * m�todo tamb�m instancia um observador de <i>leases</i>.
   * 
   * @throws CORBAException
   * @throws ACSUnavailableException
   * @throws OpenBusException
   */
  public void fetchACS() throws CORBAException, ServiceUnavailableException,
    ACSUnavailableException {
    try {
      this.acs = Utils.fetchAccessControlService(orb, host, port);
      this.lp = Utils.fetchAccessControlServiceLeaseProvider(orb, host, port);
      this.ft = Utils.fetchAccessControlServiceFaultTolerant(orb, host, port);
      this.ic = Utils.fetchAccessControlServiceIComponent(orb, host, port);
    }
    catch (ACSUnavailableException ex) {
      System.out.println("[ACSUnavailableException] " + ex.getMessage());
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
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public void init(String[] args, Properties props, String host, int port)
    throws UserException {
    if (orb != null) {
      return;
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
    this.orb = org.omg.CORBA.ORB.init(args, props);
    org.omg.CORBA.Object obj = this.orb.resolve_initial_references("RootPOA");
    this.rootPOA = POAHelper.narrow(obj);
    POAManager manager = this.rootPOA.the_POAManager();
    manager.activate();
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * defini��es de atributos realizadas. Em seguida, inicializa o Orb com
   * mecanismo de tolerancia a falhas ativado
   * 
   * @param args Conjunto de argumentos para a cria��o do ORB.
   * @param props Conjunto de propriedades para a cria��o do ORB.
   * @param host Endere�o do Servi�o de Controle de Acesso.
   * @param port Porta do Servi�o de Controle de Acesso.
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public void initWithFaultTolerance(String[] args, Properties props,
    String host, int port) throws UserException {
    if (orb != null) {
      return;
    }

    if (host == null)
      throw new IllegalArgumentException("O campo 'host' n�o pode ser null");
    if (port < 0)
      throw new IllegalArgumentException(
        "O campo 'port' n�o pode ser negativo.");
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
   * Fornece o Servi�o de Sess�o. Caso o Openbus ainda n�o tenha a refer�ncia
   * para o Servi�o de Sess�o este obtem o Servi�o a partir do Servi�o de
   * Registro.
   * 
   * @return O Servi�o de Sess�o.
   */
  public ISessionService getSessionService() {

    if (this.ss == null) {
      IRegistryService rgs = this.acs.getRegistryService();
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
      Log.COMMON.severe("Erro ao obter a credencial da requisi��o,", e);
      return null;
    }
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
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Servi�o de Registro.
   * @throws CORBAException
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
   * @throws PKIException Os dados fornecidos n�o s�o v�lidos.
   * @throws ACSUnavailableException Caso o Servi�o de Controle de Acesso n�o
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Servi�o de Registro.
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
          new LeaseRenewer(this.credential, this.lp, this.leaseExpiredCallback);
        this.leaseRenewer.start();
        return this.acs.getRegistryService();
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
   * Realiza uma tentativa de conex�o com o barramento(servi�o de controle de
   * acesso e o servi�o de registro), a partir de uma credencial.
   * 
   * @param credential A credencial.
   * 
   * @return O servi�o de registro.
   * 
   * @throws ACSUnavailableException Caso o Servi�o de Controle de Acesso n�o
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Servi�o de Registro.
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
      return this.acs.getRegistryService();
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
   * Finaliza a utiliza��o do barramento.
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
  public void addLeaseExpiredCallback(LeaseExpiredCallback lec) {
    this.leaseExpiredCallback = lec;
    this.leaseRenewer.setLeaseExpiredCallback(lec);
  }

  /**
   * Remove o observador de expira��o do <i>lease</i>.
   */
  public void removeLeaseExpiredCallback() {
    this.leaseExpiredCallback = null;
    this.leaseRenewer.setLeaseExpiredCallback(null);
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
   * Indica se o m�todo da interface dever interceptado.
   * 
   * @param iface RepID da interface.
   * @param method Nome do m�todo a ser testado.
   * 
   * @return <code>true</code> se o m�todo de ver interceptado, caso contr�rio
   *         <code>false</code>.
   */
  public boolean isInterceptable(String iface, String method) {
    Set<String> methods = ifaceMap.get(iface);
    return (methods == null) || !methods.contains(method);
  }

  public boolean isFaultToleranceEnable() {
    return isFaultToleranceEnable;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(int hostPort) {
    this.port = hostPort;
  }
}
