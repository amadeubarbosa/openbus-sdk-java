/*
 * Última alteração: $Id$
 */
package openbus.common.interceptors;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

/**
 * Armazena a credencial obtida por um membro junto ao Serviço de Controle de
 * acesso. É utilizada pelo interceptador de requisições para obter a credencial
 * de forma a inserí-la em um contexto de serviço.
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
   * A instância única do holder.
   */
  private static CredentialValueHolder holderInstance;

  /**
   * Obtém a instância única do holder.
   * 
   * @return A instância única do holder.
   */
  public static CredentialValueHolder getInstance() {
    if (holderInstance == null) {
      holderInstance = new CredentialValueHolder();
    }
    return holderInstance;
  }

  /**
   * Apenas para não permitir a criação de objetos fora da classe.
   */
  private CredentialValueHolder() {
  }

  /**
   * Define a credencial do membro.
   * 
   * @param orb O ORB que será utilizado para converter a credencial no tipo
   *        Any.
   * @param credential A credencial do membro.
   */
  public void setValue(ORB orb, Credential credential) {
    this.credential = credential;
    this.credentialValue = orb.create_any();
    CredentialHelper.insert(this.credentialValue, this.credential);
  }

  /**
   * Obtém a credencial.
   * 
   * @return A credencial.
   */
  public Credential getValue() {
    return this.credential;
  }

  /**
   * Obtém a credencial como um tipo Any.
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
   *         caso contrário.
   */
  public boolean hasValue() {
    return (this.credential != null);
  }

  /**
   * "Invalida" a credencial. Para utilizar novamente o holder, é necessário
   * executar o método {@link #setValue(ORB, Credential)} novamente.
   */
  public void invalidate() {
    this.credential = null;
    this.credentialValue = null;
  }
}