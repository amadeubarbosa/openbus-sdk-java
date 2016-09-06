package tecgraf.openbus.exception;

/**
 * Define uma exceção do OpenBus.
 * 
 * @author Tecgraf
 */
public abstract class OpenBusException extends Exception {

  /**
   * Construtor.
   * 
   * @param message Mensagem de erro.
   */
  protected OpenBusException(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param cause Exceção original.
   */
  protected OpenBusException(Throwable cause) {
    super(cause);
  }

  /**
   * Construtor.
   * 
   * @param message Mensagem de erro.
   * @param cause Exceção original.
   */
  protected OpenBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
