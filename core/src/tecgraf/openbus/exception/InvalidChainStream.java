package tecgraf.openbus.exception;

import tecgraf.openbus.CallerChain;

/**
 * Exce��o gerada quando tenta-se manipular uma cadeia ({@link CallerChain})
 * inv�lida.
 * 
 * @author Tecgraf
 */
public class InvalidChainStream extends OpenBusException {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  public InvalidChainStream(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param cause exce��o original.
   */
  public InvalidChainStream(Throwable cause) {
    super(cause);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param cause exce��o original.
   */
  public InvalidChainStream(String message, Throwable cause) {
    super(message, cause);
  }
}
