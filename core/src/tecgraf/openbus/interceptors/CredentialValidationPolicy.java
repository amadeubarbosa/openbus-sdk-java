/*
 * $Id $
 */
package tecgraf.openbus.interceptors;

/**
 * Define as pol�ticas para a valida��o de credenciais interceptadas em um
 * servidor.
 * 
 * @author Tecgraf/PUC-Rio
 */
public enum CredentialValidationPolicy {
  /**
   * Indica que as credenciais interceptadas ser�o sempre validadas.
   */
  ALWAYS,
  /**
   * Indica que as credenciais interceptadas ser�o validadas e armazenadas em um
   * cache.
   */
  CACHED,
  /**
   * Indica que as credenciais interceptadas n�o ser�o validadas.
   */
  NONE;
}
