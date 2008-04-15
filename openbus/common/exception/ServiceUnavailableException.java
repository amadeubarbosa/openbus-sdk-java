/*
 * $Id$
 */
package openbus.common.exception;

/**
 * Representa uma exce��o de servi�o indispon�vel.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServiceUnavailableException extends OpenBusException {
  /**
   * Cria uma exce��o de servi�o indispon�vel com uma mensagem associada.
   * 
   * @param message A mensagem de erro.
   */
  public ServiceUnavailableException(String message) {
    super(message);
  }

  /**
   * Cria uma exce��o de servi�o indispon�vel com uma mensagem e uma causa
   * associadas.
   * 
   * @param message A mensagem de erro.
   * @param cause A causa.
   */
  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
