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
   * A inst�ncia �nica do barramento.
   */
  private static Openbus instance;
  /**
   * O ORB.
   */
  private ORB orb;
  /**
   * Indica se o ORB j� foi finalizado.
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
   * Interface IComponent do Servi�o de Controle de Acesso.
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
   * Servi�o de registro.
   */
  private IRegistryService rgs;
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
   * Indica o estado da conex�o.
   */
  private ConnectionStates connectionState;

  /**
   * Poss�veis estados para a conex�o.
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
   * Retorna ao seu estado inicial, ou seja, desfaz as defini��es de atributos
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
   * Se conecta ao AccessControlServer por meio do endere�o e da porta. Este
   * m�todo tamb�m instancia um observador de <i>leases</i>.
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
  public void resetAndInitialize(String[] args, Properties props, String host,
    int port) throws UserException {
    if (props == null)
      throw new IllegalArgumentException("O campo 'props' n�o pode ser null");
    if (host == null)
      throw new IllegalArgumentException("O campo 'host' n�o pode ser null");
    if (port < 0)
      throw new IllegalArgumentException(
        "O campo 'port' n�o pode ser negativo.");
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
  public void finish(boolean force) {
    this.orb.shutdown(!force);
    this.orb.destroy();
    this.isORBFinished = true;
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
        Log.COMMON.severe("N�o foi poss�vel obter o servi�o de registro.", e);
      }
    }
    return this.rgs;
  }

  /**
   * Fornece o Servi�o de Sess�o. Caso o Openbus ainda n�o tenha a refer�ncia
   * para o Servi�o de Sess�o este obtem o Servi�o a partir do Servi�o de
   * Registro.
   * 
   * @return O Servi�o de Sess�o.
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
        return InvalidTypes.CREDENTIAL;
      }
      Credential requestCredential =
        CredentialHelper.extract(requestCredentialValue);
      return requestCredential;
    }
    catch (org.omg.CORBA.UserException e) {
      Log.COMMON.severe("Erro ao obter a credencial da requisi��o,", e);
      return InvalidTypes.CREDENTIAL;
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
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public IRegistryService connect(String user, String password)
    throws ACSLoginFailureException, ACSUnavailableException,
    InvalidCredentialException {
    if ((user == null) || (password == null))
      throw new IllegalArgumentException(
        "Os par�metros 'user' e 'password' n�o podem ser nulos.");
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
            "N�o foi poss�vel conectar ao barramento.");
        }
      }
      else {
        throw new ACSLoginFailureException("O barramento j� est� conectado.");
      }
    }
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
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public IRegistryService connect(String name, RSAPrivateKey privateKey,
    X509Certificate acsCertificate) throws ACSLoginFailureException,
    PKIException, ACSUnavailableException, InvalidCredentialException {
    if ((name == null) || (privateKey == null) || (acsCertificate == null))
      throw new IllegalArgumentException("Nenhum par�metro pode ser nulo.");
    synchronized (this.connectionState) {
      if (this.connectionState == ConnectionStates.DISCONNECTED) {
        if (this.acs == null) {
          fetchACS();
        }
        byte[] challenge;
        challenge = this.acs.getChallenge(name);
        if (challenge.length == 0) {
          throw new ACSLoginFailureException("Desafio inv�lido.");
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
            "N�o foi poss�vel conectar ao barramento.");
        }
      }
      else {
        throw new ACSLoginFailureException("O barramento j� est� conectado.");
      }
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
   * @throws IllegalArgumentException Caso o m�todo esteja com os argumentos
   *         incorretos.
   */
  public IRegistryService connect(Credential credential)
    throws InvalidCredentialException, ACSUnavailableException {
    if (credential == null)
      throw new IllegalArgumentException(
        "O par�metro 'credential' n�o pode ser nulo.");
    if (this.acs == null)
      fetchACS();
    this.credential = credential;
    if (this.acs.isValid(this.credential)) {
      if (this.rgs == null)
        this.rgs = this.getRegistryService();
      return this.rgs;
    }
    throw new InvalidCredentialException(new NO_PERMISSION(
      "Credencial inv�lida."));
  }

  /**
   * Desfaz a conex�o atual.
   * 
   * @return {@code true} caso a conex�o seja desfeita, ou {@code false} se
   *         nenhuma conex�o estiver ativa.
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
   * Informa o estado de conex�o com o barramento.
   * 
   * @return {@code true} caso a conex�o esteja ativa, ou {@code false}, caso
   *         contr�rio.
   */
  public boolean isConnected() {
    if (connectionState == ConnectionStates.CONNECTED)
      return true;
    return false;
  }

  /**
   * Atribui o observador para receber eventos de expira��o do <i>lease</i>.
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
   * Remove o observador de expira��o do <i>lease</i>.
   */
  public void removeLeaseExpiredCallback() {
    this.leaseExpiredCallback = null;
    if (this.connectionState == ConnectionStates.CONNECTED) {
      this.leaseRenewer.setLeaseExpiredCallback(null);
    }
  }
}
