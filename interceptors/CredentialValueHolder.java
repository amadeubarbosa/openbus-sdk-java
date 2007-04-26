/*
 * �ltima altera��o: $Id$
 */
package openbus.common.interceptors;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

/**
 * Armazena a credencial obtida por um membro junto ao Servi�o de Controle de
 * acesso. � utilizada pelo interceptador de requisi��es para obter a credencial
 * de forma a inser�-la em um contexto de servi�o.
 */
public class CredentialValueHolder {
  /**
   * A credencial de um membro.
   */
  private Credential credential;
  /**
   * A credencial de um membro, mas representada pelo tipo Any.
   */
  private Any credentialValue;

  /**
   * A inst�ncia �nica do holder.
   */
  private static CredentialValueHolder holderInstance;

  /**
   * Obt�m a inst�ncia �nica do holder.
   * 
   * @return A inst�ncia �nica do holder.
   */
  public static CredentialValueHolder getInstance() {
    if (holderInstance == null) {
      holderInstance = new CredentialValueHolder();
    }
    return holderInstance;
  }

  /**
   * Apenas para n�o permitir a cria��o de objetos fora da classe.
   */
  private CredentialValueHolder() {
  }

  /**
   * Define a credencial do membro.
   * 
   * @param orb O ORB que ser� utilizado para converter a credencial no tipo
   *        Any.
   * @param credential A credencial do membro.
   */
  public void setValue(ORB orb, Credential credential) {
    this.credential = credential;
    this.credentialValue = orb.create_any();
    CredentialHelper.insert(this.credentialValue, this.credential);
  }

  /**
   * Obt�m a credencial.
   * 
   * @return A credencial.
   */
  public Credential getValue() {
    return this.credential;
  }

  /**
   * Obt�m a credencial como um tipo Any.
   * 
   * @return A credencial como um tipo Any.
   */
  public Any getValueAny() {
    return this.credentialValue;
  }

  /**
   * Verifica se existe credencial definida.
   * 
   * @return {@code true} caso a credencia, esteja definida, ou {@code false},
   *         caso contr�rio.
   */
  public boolean hasValue() {
    return (this.credential != null);
  }

  /**
   * "Invalida" a credencial. Para utilizar novamente o holder, � necess�rio
   * executar o m�todo {@link #setValue(ORB, Credential)} novamente.
   */
  public void invalidate() {
    this.credential = null;
    this.credentialValue = null;
  }
}