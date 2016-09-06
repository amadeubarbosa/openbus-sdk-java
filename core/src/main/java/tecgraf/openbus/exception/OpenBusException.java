package tecgraf.openbus.exception;

/**
 * Define uma exce��o do OpenBus.
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
   * @param cause Exce��o original.
   */
  protected OpenBusException(Throwable cause) {
    super(cause);
  }

  /**
   * Construtor.
   * 
   * @param message Mensagem de erro.
   * @param cause Exce��o original.
   */
  protected OpenBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
