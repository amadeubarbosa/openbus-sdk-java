/*
 * $Id$
 */
package tecgraf.openbus.common.exception;

/**
 * Representa uma exce��o do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class OpenBusException extends Exception {
  /**
   * Cria uma exce��o do OpenBus.
   */
  public OpenBusException() {
    super();
  }

  /**
   * Cria uma exce��o do OpenBus com uma causa associada.
   * 
   * @param cause A causa.
   */
  public OpenBusException(Throwable cause) {
    super(cause);
  }

  /**
   * Cria uma exce��o do OpenBus com uma mensagem associada.
   * 
   * @param message A mensagem.
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
