package tecgraf.openbus.exception;

/**
 * Exceção de tentativa de uso de conexão não logada.
 * 
 * @author Tecgraf
 */
public final class NotLoggedIn extends OpenBusException {

  /**
   * Construtor.
   */
  public NotLoggedIn() {
    super("Conexão não está autenticada.");
  }

}
