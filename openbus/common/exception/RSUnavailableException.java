/*
 * $Id$
 */
package openbus.common.exception;

/**
 * Representa uma exceção de serviço de registro indisponível.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class RSUnavailableException extends ServiceUnavailableException {
  /**
   * Cria uma exceção de serviço de registro indisponível com uma mensagem
   * associada.
   * 
   * @param message A mensagem de erro.
   */
  public RSUnavailableException(String message) {
    super(message);
  }

  /**
   * Cria uma exceção de serviço de registro indisponível com uma mensagem e uma
   * causa associadas.
   * 
   * @param message A mensagem de erro.
   * @param cause A causa.
   */
  public RSUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
