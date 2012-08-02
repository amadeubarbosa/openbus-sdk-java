package tecgraf.openbus.exception;

/**
 * Exce��o indicando chave privada corrompida.
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
   * @param exception exce��o original.
   */
  public InvalidPrivateKey(String message, Throwable exception) {
    super(message, exception);
  }
}
