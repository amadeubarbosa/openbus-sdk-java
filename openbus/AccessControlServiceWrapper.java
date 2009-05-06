/*
 * $Id$
 */
package openbus;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashSet;
import java.util.Set;

import openbus.common.LeaseExpiredCallback;
import openbus.common.LeaseRenewer;
import openbus.common.Utils;
import openbus.common.exception.ACSUnavailableException;
import openbus.exception.CORBAException;
import openbus.exception.InvalidCredentialException;
import openbus.exception.PKIException;
import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHolder;
import openbusidl.acs.IAccessControlService;
import openbusidl.acs.ILeaseProvider;
import openbusidl.acs.ILeaseProviderHelper;
import openbusidl.rs.IRegistryService;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;

import scs.core.IComponent;
import scs.core.IComponentHelper;

/**
 * Encapsula o Serviço de Controle de Acesso.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class AccessControlServiceWrapper {
  /**
   * Proxy para a faceta IAccessControlService.
   */
  private IAccessControlService acs;
  /**
   * Proxy para a faceta ILeaseProvider.
   */
  private ILeaseProvider lp;
  /**
   * O Serviço de Registro.
   */
  private RegistryServiceWrapper rs;
  /**
   * O renovador do <i>lease</i>.
   */
  private LeaseRenewer leaseRenewer;
  /**
   * <i>Callback</i> para a notificação de que um <i>lease</i> expirou.
   */
  private LeaseExpiredCallbackImpl leaseExpiredCallback;

  /**
   * Cria um objeto que encapsula o Serviço de Controle de Acesso.
   * 
   * @param orb O orb utilizado para obter o serviço.
   * @param host A máquina onde o serviço está localizado.
   * @param port A porta onde o serviço está disponível.
   * 
   * @throws ACSUnavailableException Caso o serviço não seja encontrado.
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public AccessControlServiceWrapper(ORBWrapper orb, String host, int port)
    throws ACSUnavailableException, CORBAException {
    this.acs = Utils.fetchAccessControlService(orb, host, port);
    try {
      IComponent component = IComponentHelper.narrow(acs._get_component());
      this.lp =
        ILeaseProviderHelper.narrow(component.getFacetByName("ILeaseProvider"));
    }
    catch (Exception e) {

    }
    this.rs = new RegistryServiceWrapper();

    this.leaseExpiredCallback = new LeaseExpiredCallbackImpl();
  }

  /**
   * Autentica uma entidade a partir de um nome de usuário e senha.
   * 
   * @param name O nome do usuário.
   * @param password A senha do usuário.
   * 
   * @return {@code true} caso a entidade seja autenticada, ou {@code false},
   *         caso contrário.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public boolean loginByPassword(String name, String password)
    throws CORBAException {
    CredentialHolder aCredential = new CredentialHolder();
    boolean result;
    try {
      result =
        this.acs.loginByPassword(name, password, aCredential, new IntHolder());
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }

    if (result) {
      Registry.getInstance().setCredential(aCredential.value);
      this.leaseRenewer =
        new LeaseRenewer(aCredential.value, this.lp, this.leaseExpiredCallback);
      this.leaseRenewer.start();
      return true;
    }
    return false;
  }

  /**
   * Autentica uma entidade a partir de um certificado digital.
   * 
   * @param name O nome da entidade.
   * @param privateKey A chave privada da entidade.
   * @param acsCertificate O certificado digital do Serviço de Controle de
   *        Acesso.
   * 
   * @return {@code true} caso a entidade seja autenticada, ou {@code false},
   *         caso contrário.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   * @throws PKIException
   */
  public boolean loginByCertificate(String name, RSAPrivateKey privateKey,
    X509Certificate acsCertificate) throws CORBAException, PKIException {
    byte[] challenge;
    try {
      challenge = this.acs.getChallenge(name);
      if (challenge.length == 0) {
        return false;
      }
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
    byte[] answer;
    try {
      answer = Utils.generateAnswer(challenge, privateKey, acsCertificate);
    }
    catch (GeneralSecurityException e) {
      throw new PKIException(e);
    }

    CredentialHolder aCredential = new CredentialHolder();
    boolean result;
    try {
      result =
        this.acs.loginByCertificate(name, answer, aCredential, new IntHolder());
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }

    if (result) {
      Registry.getInstance().setCredential(aCredential.value);
      this.leaseRenewer =
        new LeaseRenewer(aCredential.value, this.lp, this.leaseExpiredCallback);
      this.leaseRenewer.start();
      return true;
    }
    return false;
  }

  /**
   * Autentica uma entidade a partir de uma credencial.
   * 
   * @param credential A credencial.
   * 
   * @return {@code true} caso a entidade seja autenticada, ou {@code false},
   *         caso contrário.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public boolean loginByCredential(Credential credential) throws CORBAException {
    Registry registry = Registry.getInstance();
    registry.setCredential(credential);
    return this.isValid(credential);
  }

  /**
   * Desconecta a entidade em relação ao Serviço.
   * 
   * @return {@code true}, caso a entidade seja desconectada, ou {@code false},
   *         caso contrário.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public boolean logout() throws CORBAException {
    Registry registry = Registry.getInstance();
    Credential credential = registry.getCredential();
    this.leaseRenewer.finish();
    this.leaseRenewer = null;
    try {
      boolean result = this.acs.logout(credential);
      registry.setCredential(null);
      return result;
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
  }

  /**
   * Obtém o Serviço de Registro.
   * 
   * @return O Serviço de Registro, ou {@code null}, caso o Serviço não esteja
   *         disponível.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   * @throws InvalidCredentialException Indica que a credencial da entidade não
   *         é mais válida.
   */
  public RegistryServiceWrapper getRegistryService() throws CORBAException,
    InvalidCredentialException {
    try {
      IRegistryService registryService = this.acs.getRegistryService();
      if (registryService == null) {
        return null;
      }
      this.rs.setRS(registryService);
      return this.rs;
    }
    catch (NO_PERMISSION e) {
      throw new InvalidCredentialException(e);
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
  }

  /**
   * Obtém a credencial da entidade.
   * 
   * @return A credencial da entidade.
   */
  public Credential getCredential() {
    return Registry.getInstance().getCredential();
  }

  /**
   * Verifica se uma determinada credencial é válida.
   * 
   * @param credential A credencial.
   * 
   * @return {@code true} caso a credencial seja válida, ou {@code false}, caso
   *         contrário.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public boolean isValid(Credential credential) throws CORBAException {
    try {
      return this.acs.isValid(credential);
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
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
