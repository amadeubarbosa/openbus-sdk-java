package tecgraf.openbus.exception;

/**
 * Exce��o indicativa de uso de segredo errado.
 * 
 * @author Tecgraf
 */
public class WrongSecret extends OpenBusException {

  /**
   * Constutor
   * 
   * @param message mensagem de erro.
   */
  protected WrongSecret(String message) {
    super(message);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   * @param exception exce��o original.
   */
  public WrongSecret(String message, Throwable exception) {
    super(message, exception);
  }

}
