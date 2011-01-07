/*
 * $Id $
 */
package tecgraf.openbus.interceptors;

/**
 * Define as políticas para a validação de credenciais interceptadas em um
 * servidor.
 * 
 * @author Tecgraf/PUC-Rio
 */
public enum CredentialValidationPolicy {
  /**
   * Indica que as credenciais interceptadas serão sempre validadas.
   */
  ALWAYS,
  /**
   * Indica que as credenciais interceptadas serão validadas e armazenadas em um
   * cache.
   */
  CACHED,
  /**
   * Indica que as credenciais interceptadas não serão validadas.
   */
  NONE;
}
