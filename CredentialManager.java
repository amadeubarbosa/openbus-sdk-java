/*
 * $Id$
 */
package openbus.common;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;

/**
 * Reposit�rio e gerenciador de credenciais transportadas em requisi��es de
 * servi�o. Armazena a credencial de um membro para permitir sua inser��o nas
 * requisi��es de servi�o emitidas por esse membro. Gerencia o transporte da
 * credencial recebida em uma requisi��o de servi�o para o tratador dessa
 * requisi��o.
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
   * A refer�ncia para o servi�o de controle de acesso.
   */
  private IAccessControlService accessControlService;
  /**
   * A credencial do membro.
   */
  private Credential memberCredential;
  /**
   * O valor {@link Any} correspondente � credencial do membro.
   */
  private Any memberCredentialValue;

  /** Refer�ncia para a inst�ncia �nica do gerente de credenciais */
  private static CredentialManager instance;

  /**
   * Obt�m a refer�ncia para a inst�ncia do gerente de credenciais;
   * 
   * @return a inst�ncia do gerente de credenciais
   */
  public static CredentialManager getInstance() {
    if (instance == null) {
      instance = new CredentialManager();
    }
    return instance;
  }

  /**
   * Constr�i a inst�ncia �nica do gerente de credenciais.
   */
  private CredentialManager() {
    memberCredential = null;
    memberCredentialValue = null;
    accessControlService = null;
    orb = null;
    credentialSlot = -1;
  }

  /**
   * Armazena a refer�ncia para o ORB corrente.
   * 
   * @param orb refer�ncia para o ORB
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
   * Obt�m o slot de transporte da credencial.
   * 
   * @return slot alocado para transporte da credencial
   */
  public int getCredentialSlot() {
    if (credentialSlot == -1) {
      throw new IllegalStateException();
    }
    return credentialSlot;
  }

  /**
   * Armazena refer�ncia para o servi�o de controle de acesso.
   * 
   * @param accessControlService refer�ncia para o servi�o de controle de acesso
   */
  public void setACS(IAccessControlService accessControlService) {
    this.accessControlService = accessControlService;
  }

  /**
   * Obt�m a refer�ncia para o servi�o de controle de acesso.
   * 
   * @return refer�ncia para o servi�o de controle de acesso
   */
  public IAccessControlService getACS() {
    return accessControlService;
  }

  /**
   * Armazena a credencial do membro.
   * 
   * @param memberCredential a credencial a armazenar
   */
  public void setMemberCredential(Credential memberCredential) {
    if (orb == null) {
      throw new IllegalStateException();
    }
    this.memberCredential = memberCredential;
    this.memberCredentialValue = orb.create_any();
    CredentialHelper.insert(this.memberCredentialValue, memberCredential);
  }

  /**
   * Obt�m a credencial do membro.
   * 
   * @return a credencial do membro
   */
  public Credential getMemberCredential() {
    return memberCredential;
  }

  /**
   * Obt�m o valor Any correspondente � credencial do membro corrente.
   * 
   * @return o valor Any da credencial do membro
   */
  public Any getMemberCredentialValue() {
    return memberCredentialValue;
  }

  /**
   * Verifica se existe uma credencial de membro armazenada.
   * 
   * @return true se existe credencial, false caso contr�rio
   */
  public boolean hasMemberCredential() {
    return (memberCredential != null);
  }

  /**
   * Invalida a credencial de membro armazenada.
   */
  public void invalidateMemberCredential() {
    memberCredential = null;
    memberCredentialValue = null;
  }

  /**
   * Prepara a credencial recebida em uma requisi��o de servi�o para
   * armazenamento no slot alocado para seu transporte.
   * 
   * @param credential a credencial recebida na requisi��o de servi�o
   * @return o valor (Any) a ser atribu�do ao slot
   */
  public Any getCredentialValue(Credential credential) {
    if (orb == null) {
      throw new IllegalStateException();
    }
    Any credentialValue = orb.create_any();
    CredentialHelper.insert(credentialValue, credential);
    return credentialValue;
  }

  /**
   * Recupera a credencial recebida na requisi��o de seu slot de transporte.
   * 
   * @return a credencial associada � requisi��o de servi�o
   */
  public Credential getRequestCredential() {
    if (orb == null || credentialSlot == -1) {
      throw new IllegalStateException();
    }
    try {
      Current pic = CurrentHelper.narrow(orb
        .resolve_initial_references("PICurrent"));
      Any requestCredentialValue = pic.get_slot(credentialSlot);
      Credential requestCredential = CredentialHelper
        .extract(requestCredentialValue);
      return requestCredential;
    }
    catch (org.omg.CORBA.ORBPackage.InvalidName in) {
      /* Como repassar esse erro ao servant? */
      System.out.println("CredentialManager: N�O OBTEVE REF PARA PICURRENT!!!");
    }
    catch (org.omg.PortableInterceptor.InvalidSlot is) {
      /* Como repassar esse erro ao servant? */
      System.out.println("CredentialManager: SLOT INVALIDO !!!");
    }
    return null;
  }
}
