/*
 * $Id$
 */
package openbus.common.exception;

/**
 * Representa uma exce��o do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class OpenBusException extends Exception {
  /**
   * Cria uma exce��o do OpenBus com uma mensagem associada.
   * 
   * @param message A mensagem de erro.
   */
  public OpenBusException(String message) {
    super(message);
  }

  /**
   * Cria uma exce��o do OpenBus com uma mensagem e uma causa associadas.
   * 
   * @param message A mensagem de erro.
   * @param cause A causa.
   */
  public OpenBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
