/*
 * $Id$
 */
package openbus;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import openbus.common.LeaseExpiredCallback;
import openbus.common.LeaseRenewer;
import openbus.common.Log;
import openbus.common.Utils;
import openbus.common.exception.ACSLoginFailureException;
import openbus.common.exception.ACSUnavailableException;
import openbus.common.exception.OpenBusException;
import openbus.common.interceptors.ClientInitializer;
import openbus.common.interceptors.ServerInitializer;
import openbus.exception.CORBAException;
import openbus.exception.InvalidCredentialException;
import openbus.exception.PKIException;
import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;
import openbusidl.acs.CredentialHolder;
import openbusidl.acs.IAccessControlService;
import openbusidl.acs.IAccessControlServiceHelper;
import openbusidl.acs.ILeaseProvider;
import openbusidl.acs.ILeaseProviderHelper;
import openbusidl.rs.IRegistryService;
import openbusidl.rs.Property;
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

import scs.core.IComponent;

/**
 * API de acesso a um barramento OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Registry {
  /**
   * A instância única do barramento.
   */
  private static Registry instance;
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
   * O Serviço de Controle de Acesso.
   */
  private IAccessControlService acs;
  /**
   * Interface ILeaseProvider do Serviço de Controle de Acesso.
   */
  private ILeaseProvider lp;
  /**
   * O renovador do <i>lease</i>.
   */
  private LeaseRenewer leaseRenewer;
  /**
   * <i>Callback</i> para a notificação de que um <i>lease</i> expirou.
   */
  private LeaseExpiredCallbackImpl leaseExpiredCallback;
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
  private CredentialHolder credential;
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
    CONNECTED,
    DISCONNECTED
  };

  /**
   * Retorna ao seu estado inicial, ou seja, desfaz as definições de atributos
   * realizadas.
   */
  private void reset() {
    this.threadLocalCredential = new ThreadLocal<Credential>();
    this.credential = new CredentialHolder();
    this.requestCredentialSlot = -1;
    if (!this.isORBFinished && this.orb != null)
      this.finish(true);
    this.orb = null;
    this.rootPOA = null;
    this.isORBFinished = false;
    this.acs = null;
    this.lp = null;
    this.leaseRenewer = null;
    this.leaseExpiredCallback = null;
    this.rgs = null;
    this.ss = null;
    this.connectionState = ConnectionStates.DISCONNECTED;
  }

  /**
   * Cria um ACSWrapper.
   */
  private void createACS(String ACSHost, int ACSPort)
    throws ACSUnavailableException, InvalidCredentialException {
    IComponent ic =
      Utils.fetchAccessControlServiceIComponent(orb, ACSHost, ACSPort);
    this.acs =
      IAccessControlServiceHelper.narrow(ic
        .getFacet(Utils.ACCESS_CONTROL_SERVICE_INTERFACE));
    this.lp =
      ILeaseProviderHelper.narrow(ic.getFacet(Utils.LEASE_PROVIDER_INTERFACE));

    try {
      this.rgs = this.acs.getRegistryService();
    }
    catch (NO_PERMISSION e) {
      throw new InvalidCredentialException(e);
    }

    this.leaseExpiredCallback = new LeaseExpiredCallbackImpl();
  }

  /**
   * Construtor do barramento.
   */
  private Registry() {
    this.reset();
  }

  /**
   * Fornece a instância única do barramento.
   * 
   * @return A instância única do barramento.
   */
  public static Registry getInstance() {
    if (instance == null) {
      instance = new Registry();
    }
    return instance;
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * definições de atributos realizadas. Em seguida, cria um ORBWrapper e um
   * ACSWrapper a partir dos dados recebidos.
   * 
   * @param args Conjunto de argumentos para a criação do ORB.
   * @param props Conjunto de propriedades para a criação do ORB.
   * @param ACSHost Endereço do Serviço de Controle de Acesso.
   * @param ACSPort Porta do Serviço de Controle de Acesso.
   * 
   * @throws ACSUnavailableException Caso o Serviço de Controle de Acesso não
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Serviço de Registro.
   */
  public void resetAndInitialize(String[] args, Properties props,
    String ACSHost, int ACSPort) throws ACSUnavailableException,
    InvalidCredentialException {
    reset();
    // init
    String clientInitializerClassName = ClientInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + clientInitializerClassName,
      clientInitializerClassName);
    String serverInitializerClassName = ServerInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + serverInitializerClassName,
      serverInitializerClassName);
    this.orb = org.omg.CORBA.ORB.init(args, props);
    createACS(ACSHost, ACSPort);
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
   * <p>
   * OBS: A chamada a este método ativa o POAManager.
   * 
   * @return O RootPOA.
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   */
  public POA getRootPOA() throws UserException {
    if (this.rootPOA == null) {
      org.omg.CORBA.Object obj = this.orb.resolve_initial_references("RootPOA");
      this.rootPOA = POAHelper.narrow(obj);
      POAManager manager = this.rootPOA.the_POAManager();
      manager.activate();
    }
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
    this.orb.shutdown(force);
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
    return this.rgs;
  }

  /**
   * Fornece o Serviço de Sessão, obtido a partir do Serviço de Registro.
   * 
   * @return O Serviço de Sessão.
   */
  public ISessionService getSessionService() {
    if (this.ss == null) {
      Property[] properties = new openbusidl.rs.Property[1];
      properties[0] =
        new Property(Utils.FACETS_PROPERTY_NAME,
          new String[] { Utils.SESSION_SERVICE_FACET_NAME });
      ServiceOffer[] offers = this.rgs.find(properties);
      if (offers.length > 0) {
        IComponent component = offers[0].member;
        Object facet = component.getFacet(Utils.SESSION_SERVICE_INTERFACE);
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
    if (this.credential == null)
      return null;
    return this.credential.value;
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
   * @param requestCredentialSlot O slot da credencial da requisição.
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
   * Realiza uma tentativa de conexão com o barramento, a partir de uma
   * credencial.
   * 
   * @param credential A credencial.
   * 
   * @return O serviço de registro.
   * 
   * @throws InvalidCredentialException Caso a credencial seja recusada.
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public IRegistryService connect(Credential credential)
    throws InvalidCredentialException {
    if (this.acs.isValid(credential)) {
      this.credential = new CredentialHolder(credential);
      if (this.rgs == null)
        this.rgs = this.acs.getRegistryService();
      return this.rgs;
    }
    throw new InvalidCredentialException(new NO_PERMISSION(
      "Credencial inválida."));
  }

  /**
   * Realiza uma tentativa de conexão com o barramento, via nome de usuário e
   * senha.
   * 
   * @param user Nome do usuário.
   * @param password Senha do usuário.
   * 
   * @return O serviço de registro.
   * 
   * @throws ACSLoginFailureException O par nome de usuário e senha não foram
   *         validados.
   * @throws OpenBusException O barramento ainda não foi inicializado.
   */
  public IRegistryService connect(String user, String password)
    throws ACSLoginFailureException, OpenBusException {
    synchronized (this.connectionState) {
      if (this.connectionState == ConnectionStates.DISCONNECTED) {
        if (this.acs == null) {
          throw new OpenBusException(
            "A referência ao serviço de controle de acesso está inválida. Reinicialize o barramento.");
        }
        if (this.acs.loginByPassword(user, password, this.credential,
          new IntHolder())) {
          this.leaseRenewer =
            new LeaseRenewer(this.credential.value, this.lp,
              this.leaseExpiredCallback);
          this.leaseRenewer.start();
          connectionState = ConnectionStates.CONNECTED;
          this.rgs = this.acs.getRegistryService();
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
   * Realiza uma tentativa de conexão com o barramento, via certificado.
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
   * @throws OpenBusException O barramento ainda não foi inicializado.
   */
  public IRegistryService connect(String name, RSAPrivateKey privateKey,
    X509Certificate acsCertificate) throws ACSLoginFailureException,
    PKIException, OpenBusException {
    synchronized (this.connectionState) {
      if (this.connectionState == ConnectionStates.DISCONNECTED) {
        if (this.acs == null) {
          throw new OpenBusException();
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

        if (this.acs.loginByCertificate(name, answer, this.credential,
          new IntHolder())) {
          this.leaseRenewer =
            new LeaseRenewer(this.credential.value, this.lp,
              this.leaseExpiredCallback);
          this.leaseRenewer.start();
          connectionState = ConnectionStates.CONNECTED;
          this.rgs = this.acs.getRegistryService();
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
   * Desfaz a conexão atual.
   * 
   * @return {@code true} caso a conexão seja desfeita.
   * @return {@code false} se nenhuma conexão estiver ativa.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public boolean disconnect() throws SystemException {
    synchronized (this.connectionState) {
      if (this.connectionState == ConnectionStates.CONNECTED) {
        boolean status = false;
        try {
          this.leaseRenewer.finish();
          this.leaseRenewer = null;
          status = this.acs.logout(this.credential.value);
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
   * @return {@code true} caso a conexão esteja ativa.
   * @return {@code false} caso contrário.
   */
  public boolean isConnected() {
    if (connectionState == ConnectionStates.CONNECTED)
      return true;
    return false;
  }

  /**
   * Adiciona um observador para receber eventos de expiração do <i>lease</i>.
   * 
   * @param lec O observador.
   * 
   * @return {@code true}, caso o observador seja adicionado, ou {@code false},
   *         caso contrário.
   */
  public boolean addLeaseExpiredCallback(LeaseExpiredCallback lec) {
    return this.leaseExpiredCallback.addLeaseExpiredCallback(lec);
  }

  /**
   * Remove um observador de expiração do <i>lease</i>.
   * 
   * @param lec O observador.
   * 
   * @return {@code true}, caso o observador seja removido, ou {@code false},
   *         caso contrário.
   */
  public boolean removeLeaseExpiredCallback(LeaseExpiredCallback lec) {
    return this.leaseExpiredCallback.removeLeaseExpiredCallback(lec);
  }

  /**
   * Implementa uma <i>callback</i> para a notificação de que um <i>lease</i>
   * expirou.
   * 
   * @author Tecgraf/PUC-Rio
   */
  private static class LeaseExpiredCallbackImpl implements LeaseExpiredCallback {
    /**
     * Observadores da expiração do <i>lease</i>.
     */
    private Set<LeaseExpiredCallback> callbacks;

    /**
     * Cria a <i>callback</i>.
     */
    LeaseExpiredCallbackImpl() {
      this.callbacks = new HashSet<LeaseExpiredCallback>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void expired() {
      for (LeaseExpiredCallback lec : this.callbacks) {
        lec.expired();
      }
    }

    /**
     * Adiciona um observador para receber eventos de expiração do <i>lease</i>.
     * 
     * @param lec O observador.
     * 
     * @return {@code true}, caso o observador seja adicionado, ou {@code false}
     *         , caso contrário.
     */
    boolean addLeaseExpiredCallback(LeaseExpiredCallback lec) {
      return this.callbacks.add(lec);
    }

    /**
     * Remove um observador de expiração do <i>lease</i>.
     * 
     * @param lec O observador.
     * 
     * @return {@code true}, caso o observador seja removido, ou {@code false},
     *         caso contrário.
     */
    boolean removeLeaseExpiredCallback(LeaseExpiredCallback lec) {
      return this.callbacks.remove(lec);
    }
  }
}
