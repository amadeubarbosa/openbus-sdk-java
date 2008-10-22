/*
 * $Id$
 */
package openbus;

import openbus.common.Log;
import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;
import openbusidl.ss.ISession;

import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableServer.POA;

/**
 * Oferece um registro onde ficam armazenados os objetos de uso geral para o
 * acesso ao barramento e à infra-estrutura CORBA.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Registry {
  /**
   * A instância única do registro.
   */
  private static Registry instance;
  /**
   * O ORB.
   */
  private ORBWrapper orbWrapper;
  /**
   * O POA.
   */
  private POA poa;
  /**
   * O Serviço de Controle de Acesso.
   */
  private AccessControlServiceWrapper acs;
  /**
   * A credencial da entidade.
   */
  private Credential credential;
  /**
   * A credencial da enteidade, válida apenas na <i>thread</i> corrente.
   */
  private ThreadLocal<Credential> threadLocalCredential;
  /**
   * O slot da credencial da requisição.
   */
  private int requestCredentialSlot;
  /**
   * A sessão da entidade.
   */
  private ISession session;

  /**
   * Obtém a instância única do registro.
   * 
   * @return A instância única do registro.
   */
  public static Registry getInstance() {
    if (instance == null) {
      instance = new Registry();
    }
    return instance;
  }

  /**
   * Cria o registro.
   */
  private Registry() {
    this.reset();
  }

  /**
   * Define o ORB.
   * 
   * @param orbWrapper O ORB.
   */
  public void setORBWrapper(ORBWrapper orbWrapper) {
    this.orbWrapper = orbWrapper;
  }

  /**
   * Obtém o ORB.
   * 
   * @return O ORB.
   */
  public ORBWrapper getORBWrapper() {
    return this.orbWrapper;
  }

  /**
   * Define o POA.
   * 
   * @param poa O POA.
   */
  public void setPOA(POA poa) {
    this.poa = poa;
  }

  /**
   * Obtém o POA.
   * 
   * @return O POA.
   */
  public POA getPOA() {
    return this.poa;
  }

  /**
   * Define o Serviço de Controle de Acesso.
   * 
   * @param acs O Serviço de Controle de Acesso.
   */
  public void setACS(AccessControlServiceWrapper acs) {
    this.acs = acs;
  }

  /**
   * Obtém o Serviço de Controle de Acesso.
   * 
   * @return the acs
   */
  public AccessControlServiceWrapper getACS() {
    return this.acs;
  }

  /**
   * Define a credencial da entidade.
   * 
   * @param credential A credencial.
   */
  void setCredential(Credential credential) {
    this.credential = credential;
  }

  /**
   * Obtém a credencial da entidade.
   * 
   * @return A credencial.
   */
  public Credential getCredential() {
    Credential threadCredential = this.threadLocalCredential.get();
    if (threadCredential != null) {
      return threadCredential;
    }
    return this.credential;
  }

  /**
   * @param credential
   */
  public void setThreadCredential(Credential credential) {
    this.threadLocalCredential.set(credential);
  }

  /**
   * Define o slot da credencial da requisição atual.
   * 
   * @param requestCredentialSlot O slot da credencial da requisição.
   */
  public void setRequestCredentialSlot(int requestCredentialSlot) {
    this.requestCredentialSlot = requestCredentialSlot;
  }

  /**
   * Obtém a credencial associada à requisição atual.
   * 
   * @return A credencial da requisição.
   */
  public Credential getRequestCredential() {
    try {
      Current pic =
        CurrentHelper.narrow(this.orbWrapper.getORB()
          .resolve_initial_references("PICurrent"));
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
   * Define a sessão da entidade.
   * 
   * @param session A sessão da entidade.
   */
  public void setSession(ISession session) {
    this.session = session;
  }

  /**
   * Obtém a sessão da entidade.
   * 
   * @return A sessão da entidade.
   */
  public ISession getSession() {
    return this.session;
  }

  /**
   * Retorna para o seu estado inicial, ou seja, desfaz as definições de
   * atributos realizadas.
   */
  public void reset() {
    this.threadLocalCredential = new ThreadLocal<Credential>();
    this.requestCredentialSlot = -1;
    this.orbWrapper = null;
    this.poa = null;
    this.acs = null;
    this.credential = null;
    this.session = null;
  }
}
