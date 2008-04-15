/*
 * $Id$
 */
package openbus.common.exception;

/**
 * Representa uma exce��o de servi�o de registro indispon�vel.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class RSUnavailableException extends ServiceUnavailableException {
  /**
   * Cria uma exce��o de servi�o de registro indispon�vel com uma mensagem
   * associada.
   * 
   * @param message A mensagem de erro.
   */
  public RSUnavailableException(String message) {
    super(message);
  }

  /**
   * Cria uma exce��o de servi�o de registro indispon�vel com uma mensagem e uma
   * causa associadas.
   * 
   * @param message A mensagem de erro.
   * @param cause A causa.
   */
  public RSUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
