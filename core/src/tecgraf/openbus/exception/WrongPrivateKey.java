package tecgraf.openbus.exception;

/**
 * Exceção indicando chave privada não corresponde à chave pública ou
 * certificado utilizado.
 * 
 * @author Tecgraf
 */
public class WrongPrivateKey extends OpenBusException {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  protected WrongPrivateKey(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param exception exceção original.
   */
  public WrongPrivateKey(String message, Throwable exception) {
    super(message, exception);
  }

}
