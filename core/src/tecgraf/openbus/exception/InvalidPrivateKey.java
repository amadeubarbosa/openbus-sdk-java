package tecgraf.openbus.exception;

/**
 * Exceção indicando chave privada corrompida.
 * 
 * @author Tecgraf
 */
public final class InvalidPrivateKey extends OpenBusException {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  public InvalidPrivateKey(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param exception exceção original.
   */
  public InvalidPrivateKey(String message, Throwable exception) {
    super(message, exception);
  }
}
