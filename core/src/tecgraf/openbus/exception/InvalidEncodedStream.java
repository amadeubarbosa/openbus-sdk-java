package tecgraf.openbus.exception;


/**
 * Exceção gerada quando tenta-se manipular uma stream de dados inválida.
 * 
 * @author Tecgraf
 */
public class InvalidEncodedStream extends OpenBusException {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  public InvalidEncodedStream(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param cause exceção original.
   */
  public InvalidEncodedStream(Throwable cause) {
    super(cause);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param cause exceção original.
   */
  public InvalidEncodedStream(String message, Throwable cause) {
    super(message, cause);
  }
}
