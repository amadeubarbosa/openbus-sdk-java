package tecgraf.openbus.exception;

/**
 * Exce��o com uso de criptografia.
 * 
 * @author Tecgraf
 */
public final class CryptographyException extends OpenBusException {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  public CryptographyException(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param cause exe��o original
   */
  public CryptographyException(Throwable cause) {
    super(cause);
  }
}
