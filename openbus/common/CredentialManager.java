/*
 * $Id$
 */
package openbus.common;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;

/**
 * Repositório e gerenciador de credenciais transportadas em requisições de
 * serviço. Armazena a credencial de um membro para permitir sua inserção nas
 * requisições de serviço emitidas por esse membro. Gerencia o transporte da
 * credencial recebida em uma requisição de serviço para o tratador dessa
 * requisição.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class CredentialManager {
  /**
   * O ORB.
   */
  private ORB orb;
  /**
   * O slot de transporte da credencial.
   */
  private int credentialSlot;
  /**
   * A referência para o serviço de controle de acesso.
   */
  private IAccessControlService accessControlService;
  /**
   * A credencial do membro.
   */
  private Credential memberCredential;
  /**
   * O valor {@link Any} correspondente à credencial do membro.
   */
  private Any memberCredentialValue;

  /** Referência para a instância única do gerente de credenciais */
  private static CredentialManager instance;
  /**
   * A credencial a ser utilizada numa determinada thread.
   */
  private static ThreadLocal<Any> memberCredentialValueThread =
    new ThreadLocal<Any>();

  /**
   * Obtém a referência para a instância do gerente de credenciais;
   * 
   * @return a instância do gerente de credenciais
   */
  public static CredentialManager getInstance() {
    if (instance == null) {
      instance = new CredentialManager();
    }
    return instance;
  }

  /**
   * Constrói a instância única do gerente de credenciais.
   */
  private CredentialManager() {
    this.credentialSlot = -1;
  }

  /**
   * Armazena a referência para o ORB corrente.
   * 
   * @param orb referência para o ORB
   */
  public void setORB(ORB orb) {
    this.orb = orb;
  }

  /**
   * Armazena o slot de transporte da credencial.
   * 
   * @param credentialSlot O slot alocado para transporte da credencial.
   */
  public void setCredentialSlot(int credentialSlot) {
    this.credentialSlot = credentialSlot;
  }

  /**
   * Obtém o slot de transporte da credencial.
   * 
   * @return slot alocado para transporte da credencial
   */
  public int getCredentialSlot() {
    if (this.credentialSlot == -1) {
      throw new IllegalStateException();
    }
    return this.credentialSlot;
  }

  /**
   * Armazena referência para o serviço de controle de acesso.
   * 
   * @param accessControlService referência para o serviço de controle de acesso
   */
  public void setACS(IAccessControlService accessControlService) {
    this.accessControlService = accessControlService;
  }

  /**
   * Obtém a referência para o serviço de controle de acesso.
   * 
   * @return referência para o serviço de controle de acesso
   */
  public IAccessControlService getACS() {
    return this.accessControlService;
  }

  /**
   * Armazena a credencial do membro.
   * 
   * @param memberCredential a credencial a armazenar
   */
  public void setMemberCredential(Credential memberCredential) {
    if (this.orb == null) {
      throw new IllegalStateException();
    }
    this.memberCredential = memberCredential;
    this.memberCredentialValue = this.orb.create_any();
    CredentialHelper.insert(this.memberCredentialValue, memberCredential);
  }

  /**
   * Obtém a credencial do membro.
   * 
   * @return a credencial do membro
   */
  public Credential getMemberCredential() {
    return this.memberCredential;
  }

  /**
   * Obtém o valor Any correspondente à credencial do membro corrente. <br>
   * 
   * OBS: Este método deve ser utilizado apenas pelo interceptador.
   * 
   * @return o valor Any da credencial do membro
   */
  public Any getMemberCredentialValue() {
    Any memberCredentialThread = memberCredentialValueThread.get();
    if (memberCredentialThread != null) {
      Log.INTERCEPTORS.info("Utilizando a credencial definida na thread.");
      return memberCredentialThread;
    }
    Log.INTERCEPTORS.info("Utilizando a credencial original.");
    return this.memberCredentialValue;
  }

  /**
   * Verifica se existe uma credencial de membro armazenada.
   * 
   * @return true se existe credencial, false caso contrário
   */
  public boolean hasMemberCredential() {
    return (this.memberCredential != null);
  }

  /**
   * Invalida a credencial de membro armazenada.
   */
  public void invalidateMemberCredential() {
    this.memberCredential = null;
    this.memberCredentialValue = null;
  }

  /**
   * Prepara a credencial recebida em uma requisição de serviço para
   * armazenamento no slot alocado para seu transporte.
   * 
   * @param credential a credencial recebida na requisição de serviço
   * @return o valor (Any) a ser atribuído ao slot
   */
  public Any getCredentialValue(Credential credential) {
    if (this.orb == null) {
      throw new IllegalStateException();
    }
    Any credentialValue = this.orb.create_any();
    CredentialHelper.insert(credentialValue, credential);
    return credentialValue;
  }

  /**
   * Recupera a credencial recebida na requisição de seu slot de transporte.
   * 
   * @return a credencial associada à requisição de serviço
   */
  public Credential getRequestCredential() {
    if (this.orb == null || this.credentialSlot == -1) {
      throw new IllegalStateException();
    }
    try {
      Current pic =
        CurrentHelper.narrow(this.orb.resolve_initial_references("PICurrent"));
      Any requestCredentialValue = pic.get_slot(this.credentialSlot);
      if (requestCredentialValue.type().kind().equals(TCKind.tk_null)) {
        return null;
      }
      Credential requestCredential =
        CredentialHelper.extract(requestCredentialValue);
      return requestCredential;
    }
    catch (org.omg.CORBA.ORBPackage.InvalidName in) {
      /* Como repassar esse erro ao servant? */
      Log.COMMON.severe("CredentialManager: NÂO OBTEVE REF PARA PICURRENT!!!",
        in);
    }
    catch (org.omg.PortableInterceptor.InvalidSlot is) {
      /* Como repassar esse erro ao servant? */
      Log.COMMON.severe("CredentialManager: SLOT INVALIDO !!!", is);
    }
    return null;
  }

  /**
   * Define a credencial que será utilizada pelo membro na thread corrente.
   * 
   * @param credential A credencial a ser utilizada na thread corrente.
   */
  public void setMemberCredentialThread(Credential credential) {
    Any any = this.orb.create_any();
    CredentialHelper.insert(any, credential);
    memberCredentialValueThread.set(any);
  }
}
