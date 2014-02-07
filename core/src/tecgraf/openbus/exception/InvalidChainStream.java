package tecgraf.openbus.exception;

import tecgraf.openbus.CallerChain;

/**
 * Exceção gerada quando tenta-se manipular uma cadeia ({@link CallerChain})
 * inválida.
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
   * @param cause exceção original.
   */
  public InvalidChainStream(Throwable cause) {
    super(cause);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param cause exceção original.
   */
  public InvalidChainStream(String message, Throwable cause) {
    super(message, cause);
  }
}
