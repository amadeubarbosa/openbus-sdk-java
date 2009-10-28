/*
 * $Id$
 */
package tecgraf.openbus;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;
import openbusidl.acs.CredentialHolder;
import openbusidl.acs.IAccessControlService;
import openbusidl.acs.ILeaseProvider;
import openbusidl.rs.IRegistryService;
import openbusidl.rs.IRegistryServiceHelper;
import openbusidl.rs.ServiceOffer;
import openbusidl.ss.ISessionService;
import openbusidl.ss.ISessionServiceHelper;

import org.omg.CORBA.Any;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;

import scs.core.ConnectionDescription;
import scs.core.IComponent;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidName;
import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.InvalidCredentialException;
import tecgraf.openbus.exception.PKIException;
import tecgraf.openbus.interceptors.ClientInitializer;
import tecgraf.openbus.interceptors.ServerInitializer;
import tecgraf.openbus.lease.LeaseExpiredCallback;
import tecgraf.openbus.lease.LeaseRenewer;
import tecgraf.openbus.util.InvalidTypes;
import tecgraf.openbus.util.Log;
import tecgraf.openbus.util.Utils;

/**
 * API de acesso a um barramento OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Openbus {
  /**
   * A instância única do barramento.
   */
  private static Openbus instance;
  /**
   * O ORB.
   */
  private ORB orb;
  /**
   * Indica se o ORB já foi finalizado.
   */
  private boolean isORBFinished;
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
   * Interface IComponent do Serviço de Controle de Acesso.
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
   * Indica o estado da conexão.
   */
  private ConnectionStates connectionState;

  /**
   * Possíveis estados para a conexão.
   */
  private enum ConnectionStates {
    /**
     * Estado conectado.
     */
    CONNECTED,
    /**
     * Estado desconectado.
     */
    DISCONNECTED
  };

  /**
   * Retorna ao seu estado inicial, ou seja, desfaz as definições de atributos
   * realizadas.
   */
  private void reset() {
    this.threadLocalCredential = new ThreadLocal<Credential>();
    this.credential = null;
    this.requestCredentialSlot = -1;
    if (!this.isORBFinished && this.orb != null)
      this.finish(true);
    this.orb = null;
    this.rootPOA = null;
    this.isORBFinished = false;
    this.acs = null;
    this.host = null;
    this.port = -1;
    this.lp = null;
    this.ic = null;
    this.leaseRenewer = null;
    this.leaseExpiredCallback = null;
    this.rgs = null;
    this.ss = null;
    this.connectionState = ConnectionStates.DISCONNECTED;
  }

  /**
   * Se conecta ao AccessControlServer por meio do endereço e da porta. Este
   * método também instancia um observador de <i>leases</i>.
   * 
   * @throws ACSUnavailableException
   */
  private void fetchACS() throws ACSUnavailableException {
    this.acs = Utils.fetchAccessControlService(orb, host, port);
    this.lp = Utils.fetchAccessControlServiceLeaseProvider(orb, host, port);
    this.ic = Utils.fetchAccessControlServiceIComponent(orb, host, port);
  }

  /**
   * Construtor do barramento.
   */
  private Openbus() {
    this.reset();
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
  public void resetAndInitialize(String[] args, Properties props, String host,
    int port) throws UserException {
    if (props == null)
      throw new IllegalArgumentException("O campo 'props' não pode ser null");
    if (host == null)
      throw new IllegalArgumentException("O campo 'host' não pode ser null");
    if (port < 0)
      throw new IllegalArgumentException(
        "O campo 'port' não pode ser negativo.");
    reset();
    // init
    this.host = host;
    this.port = port;
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
  public void finish(boolean force) {
    this.orb.shutdown(!force);
    this.orb.destroy();
    this.isORBFinished = true;
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
    if (this.rgs == null && this.ic != null) {
      Object objRecep = this.ic.getFacetByName("IReceptacles");
      IReceptacles irecep = IReceptaclesHelper.narrow(objRecep);
      try {
        ConnectionDescription[] connections =
          irecep.getConnections("RegistryServiceReceptacle");
        if (connections.length > 0) {
          Object objref = connections[0].objref;
          this.rgs = IRegistryServiceHelper.narrow(objref);
        }
      }
      catch (InvalidName e) {
        Log.COMMON.severe("Não foi possível obter o serviço de registro.", e);
      }
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
    if (this.ss == null && this.rgs != null) {
      ServiceOffer[] offers =
        this.rgs.find(new String[] { Utils.SESSION_SERVICE_FACET_NAME });
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
        return InvalidTypes.CREDENTIAL;
      }
      Credential requestCredential =
        CredentialHelper.extract(requestCredentialValue);
      return requestCredential;
    }
    catch (org.omg.CORBA.UserException e) {
      Log.COMMON.severe("Erro ao obter a credencial da requisição,", e);
      return InvalidTypes.CREDENTIAL;
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
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Serviço de Registro.
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public IRegistryService connect(String user, String password)
    throws ACSLoginFailureException, ACSUnavailableException,
    InvalidCredentialException {
    if ((user == null) || (password == null))
      throw new IllegalArgumentException(
        "Os parâmetros 'user' e 'password' não podem ser nulos.");
    synchronized (this.connectionState) {
      if (this.connectionState == ConnectionStates.DISCONNECTED) {
        if (this.acs == null) {
          fetchACS();
        }
        CredentialHolder credentialHolder = new CredentialHolder();
        if (this.acs.loginByPassword(user, password, credentialHolder,
          new IntHolder())) {
          this.credential = credentialHolder.value;
          this.leaseRenewer =
            new LeaseRenewer(this.credential, this.lp,
              this.leaseExpiredCallback);
          this.leaseRenewer.start();
          connectionState = ConnectionStates.CONNECTED;
          this.rgs = this.getRegistryService();
          return this.rgs;
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
   * @throws PKIException Os dados fornecidos não são válidos.
   * @throws ACSUnavailableException Caso o Serviço de Controle de Acesso não
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Serviço de Registro.
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public IRegistryService connect(String name, RSAPrivateKey privateKey,
    X509Certificate acsCertificate) throws ACSLoginFailureException,
    PKIException, ACSUnavailableException, InvalidCredentialException {
    if ((name == null) || (privateKey == null) || (acsCertificate == null))
      throw new IllegalArgumentException("Nenhum parâmetro pode ser nulo.");
    synchronized (this.connectionState) {
      if (this.connectionState == ConnectionStates.DISCONNECTED) {
        if (this.acs == null) {
          fetchACS();
        }
        byte[] challenge;
        challenge = this.acs.getChallenge(name);
        if (challenge.length == 0) {
          throw new ACSLoginFailureException("Desafio inválido.");
        }
        byte[] answer;
        try {
          answer = Utils.generateAnswer(challenge, privateKey, acsCertificate);
        }
        catch (GeneralSecurityException e) {
          throw new PKIException(e);
        }

        CredentialHolder credentialHolder = new CredentialHolder();
        if (this.acs.loginByCertificate(name, answer, credentialHolder,
          new IntHolder())) {
          this.credential = credentialHolder.value;
          this.leaseRenewer =
            new LeaseRenewer(this.credential, this.lp,
              this.leaseExpiredCallback);
          this.leaseRenewer.start();
          connectionState = ConnectionStates.CONNECTED;
          this.rgs = this.getRegistryService();
          return this.rgs;
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
   * @throws IllegalArgumentException Caso o método esteja com os argumentos
   *         incorretos.
   */
  public IRegistryService connect(Credential credential)
    throws InvalidCredentialException, ACSUnavailableException {
    if (credential == null)
      throw new IllegalArgumentException(
        "O parâmetro 'credential' não pode ser nulo.");
    if (this.acs == null)
      fetchACS();
    this.credential = credential;
    if (this.acs.isValid(this.credential)) {
      if (this.rgs == null)
        this.rgs = this.getRegistryService();
      return this.rgs;
    }
    throw new InvalidCredentialException(new NO_PERMISSION(
      "Credencial inválida."));
  }

  /**
   * Desfaz a conexão atual.
   * 
   * @return {@code true} caso a conexão seja desfeita, ou {@code false} se
   *         nenhuma conexão estiver ativa.
   * 
   * @throws SystemException
   */
  public boolean disconnect() throws SystemException {
    synchronized (this.connectionState) {
      if (this.connectionState == ConnectionStates.CONNECTED) {
        boolean status = false;
        try {
          this.leaseRenewer.finish();
          this.leaseRenewer = null;
          status = this.acs.logout(this.credential);
        }
        catch (SystemException e) {
          this.connectionState = ConnectionStates.CONNECTED;
          throw e;
        }
        if (status) {
          reset();
        }
        else {
          this.connectionState = ConnectionStates.CONNECTED;
        }
        return status;
      }
      else {
        return false;
      }
    }
  }

  /**
   * Informa o estado de conexão com o barramento.
   * 
   * @return {@code true} caso a conexão esteja ativa, ou {@code false}, caso
   *         contrário.
   */
  public boolean isConnected() {
    if (connectionState == ConnectionStates.CONNECTED)
      return true;
    return false;
  }

  /**
   * Atribui o observador para receber eventos de expiração do <i>lease</i>.
   * 
   * @param lec O observador.
   * 
   */
  public void addLeaseExpiredCallback(LeaseExpiredCallback lec) {
    this.leaseExpiredCallback = lec;
    if (this.connectionState == ConnectionStates.CONNECTED) {
      this.leaseRenewer.setLeaseExpiredCallback(lec);
    }
  }

  /**
   * Remove o observador de expiração do <i>lease</i>.
   */
  public void removeLeaseExpiredCallback() {
    this.leaseExpiredCallback = null;
    if (this.connectionState == ConnectionStates.CONNECTED) {
      this.leaseRenewer.setLeaseExpiredCallback(null);
    }
  }
}
