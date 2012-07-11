package tecgraf.openbus.exception;

/**
 * Exceção indicando chave privada corrompida.
 * 
 * @author Tecgraf
 */
public final class CorruptedPrivateKey extends OpenBusException {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  public CorruptedPrivateKey(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param exception exceção original.
   */
  public CorruptedPrivateKey(String message, Throwable exception) {
    super(message, exception);
  }
}
