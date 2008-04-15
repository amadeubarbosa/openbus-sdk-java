/*
 * $Id$
 */
package openbus.common.exception;

/**
 * Representa uma exceção de serviço indisponível.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServiceUnavailableException extends OpenBusException {
  /**
   * Cria uma exceção de serviço indisponível com uma mensagem associada.
   * 
   * @param message A mensagem de erro.
   */
  public ServiceUnavailableException(String message) {
    super(message);
  }

  /**
   * Cria uma exceção de serviço indisponível com uma mensagem e uma causa
   * associadas.
   * 
   * @param message A mensagem de erro.
   * @param cause A causa.
   */
  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
