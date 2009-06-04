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
   * A inst�ncia �nica do barramento.
   */
  private static Registry instance;
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
   * O Servi�o de Controle de Acesso.
   */
  private IAccessControlService acs;
  /**
   * Interface ILeaseProvider do Servi�o de Controle de Acesso.
   */
  private ILeaseProvider lp;
  /**
   * O renovador do <i>lease</i>.
   */
  private LeaseRenewer leaseRenewer;
  /**
   * <i>Callback</i> para a notifica��o de que um <i>lease</i> expirou.
   */
  private LeaseExpiredCallbackImpl leaseExpiredCallback;
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
  private CredentialHolder credential;
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
    CONNECTED,
    DISCONNECTED
  };

  /**
   * Retorna ao seu estado inicial, ou seja, desfaz as defini��es de atributos
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
   * Fornece a inst�ncia �nica do barramento.
   * 
   * @return A inst�ncia �nica do barramento.
   */
  public static Registry getInstance() {
    if (instance == null) {
      instance = new Registry();
    }
    return instance;
  }

  /**
   * Retorna o barramento para o seu estado inicial, ou seja, desfaz as
   * defini��es de atributos realizadas. Em seguida, cria um ORBWrapper e um
   * ACSWrapper a partir dos dados recebidos.
   * 
   * @param args Conjunto de argumentos para a cria��o do ORB.
   * @param props Conjunto de propriedades para a cria��o do ORB.
   * @param ACSHost Endere�o do Servi�o de Controle de Acesso.
   * @param ACSPort Porta do Servi�o de Controle de Acesso.
   * 
   * @throws ACSUnavailableException Caso o Servi�o de Controle de Acesso n�o
   *         consiga ser contactado.
   * @throws InvalidCredentialException Caso a credencial seja rejeitada ao
   *         tentar obter o Servi�o de Registro.
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
   * Obt�m o RootPOA.
   * 
   * <p>
   * OBS: A chamada a este m�todo ativa o POAManager.
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
   * Finaliza a execu��o do ORB.
   * 
   * @param force Se a finaliza��o deve ser for�ada ou n�o.
   */
  public void finish(boolean force) {
    this.orb.shutdown(force);
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
    return this.rgs;
  }

  /**
   * Fornece o Servi�o de Sess�o, obtido a partir do Servi�o de Registro.
   * 
   * @return O Servi�o de Sess�o.
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
   * @param requestCredentialSlot O slot da credencial da requisi��o.
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
   * Realiza uma tentativa de conex�o com o barramento, a partir de uma
   * credencial.
   * 
   * @param credential A credencial.
   * 
   * @return O servi�o de registro.
   * 
   * @throws InvalidCredentialException Caso a credencial seja recusada.
   * @throws CORBAException Caso ocorra alguma exce��o na infra-estrutura CORBA.
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
      "Credencial inv�lida."));
  }

  /**
   * Realiza uma tentativa de conex�o com o barramento, via nome de usu�rio e
   * senha.
   * 
   * @param user Nome do usu�rio.
   * @param password Senha do usu�rio.
   * 
   * @return O servi�o de registro.
   * 
   * @throws ACSLoginFailureException O par nome de usu�rio e senha n�o foram
   *         validados.
   * @throws OpenBusException O barramento ainda n�o foi inicializado.
   */
  public IRegistryService connect(String user, String password)
    throws ACSLoginFailureException, OpenBusException {
    synchronized (this.connectionState) {
      if (this.connectionState == ConnectionStates.DISCONNECTED) {
        if (this.acs == null) {
          throw new OpenBusException(
            "A refer�ncia ao servi�o de controle de acesso est� inv�lida. Reinicialize o barramento.");
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
            "N�o foi poss�vel conectar ao barramento.");
        }
      }
      else {
        throw new ACSLoginFailureException("O barramento j� est� conectado.");
      }
    }
  }

  /**
   * Realiza uma tentativa de conex�o com o barramento, via certificado.
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
   * @throws OpenBusException O barramento ainda n�o foi inicializado.
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
          throw new ACSLoginFailureException("Desafio inv�lido.");
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
            "N�o foi poss�vel conectar ao barramento.");
        }
      }
      else {
        throw new ACSLoginFailureException("O barramento j� est� conectado.");
      }
    }
  }

  /**
   * Desfaz a conex�o atual.
   * 
   * @return {@code true} caso a conex�o seja desfeita.
   * @return {@code false} se nenhuma conex�o estiver ativa.
   * 
   * @throws CORBAException Caso ocorra alguma exce��o na infra-estrutura CORBA.
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
   * Informa o estado de conex�o com o barramento.
   * 
   * @return {@code true} caso a conex�o esteja ativa.
   * @return {@code false} caso contr�rio.
   */
  public boolean isConnected() {
    if (connectionState == ConnectionStates.CONNECTED)
      return true;
    return false;
  }

  /**
   * Adiciona um observador para receber eventos de expira��o do <i>lease</i>.
   * 
   * @param lec O observador.
   * 
   * @return {@code true}, caso o observador seja adicionado, ou {@code false},
   *         caso contr�rio.
   */
  public boolean addLeaseExpiredCallback(LeaseExpiredCallback lec) {
    return this.leaseExpiredCallback.addLeaseExpiredCallback(lec);
  }

  /**
   * Remove um observador de expira��o do <i>lease</i>.
   * 
   * @param lec O observador.
   * 
   * @return {@code true}, caso o observador seja removido, ou {@code false},
   *         caso contr�rio.
   */
  public boolean removeLeaseExpiredCallback(LeaseExpiredCallback lec) {
    return this.leaseExpiredCallback.removeLeaseExpiredCallback(lec);
  }

  /**
   * Implementa uma <i>callback</i> para a notifica��o de que um <i>lease</i>
   * expirou.
   * 
   * @author Tecgraf/PUC-Rio
   */
  private static class LeaseExpiredCallbackImpl implements LeaseExpiredCallback {
    /**
     * Observadores da expira��o do <i>lease</i>.
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
     * Adiciona um observador para receber eventos de expira��o do <i>lease</i>.
     * 
     * @param lec O observador.
     * 
     * @return {@code true}, caso o observador seja adicionado, ou {@code false}
     *         , caso contr�rio.
     */
    boolean addLeaseExpiredCallback(LeaseExpiredCallback lec) {
      return this.callbacks.add(lec);
    }

    /**
     * Remove um observador de expira��o do <i>lease</i>.
     * 
     * @param lec O observador.
     * 
     * @return {@code true}, caso o observador seja removido, ou {@code false},
     *         caso contr�rio.
     */
    boolean removeLeaseExpiredCallback(LeaseExpiredCallback lec) {
      return this.callbacks.remove(lec);
    }
  }
}
