package tecgraf.openbus.exception;

/**
 * Categoria de exce��o do OpenBus
 * 
 * @author Tecgraf
 */
public abstract class OpenBusException extends Exception {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  protected OpenBusException(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param cause exce��o original.
   */
  protected OpenBusException(Throwable cause) {
    super(cause);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param cause exce��o original.
   */
  protected OpenBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
