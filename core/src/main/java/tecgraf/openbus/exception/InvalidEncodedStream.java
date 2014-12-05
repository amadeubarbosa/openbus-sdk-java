package tecgraf.openbus.exception;


/**
 * Exce��o gerada quando tenta-se manipular uma stream de dados inv�lida.
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
   * @param cause exce��o original.
   */
  public InvalidEncodedStream(Throwable cause) {
    super(cause);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param cause exce��o original.
   */
  public InvalidEncodedStream(String message, Throwable cause) {
    super(message, cause);
  }
}
