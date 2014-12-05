package tecgraf.openbus.exception;

/**
 * Exce��o de tentativa de uso de conex�o n�o logada.
 * 
 * @author Tecgraf
 */
public final class NotLoggedIn extends OpenBusException {

  /**
   * Construtor.
   */
  public NotLoggedIn() {
    super("Conex�o n�o est� autenticada.");
  }

}
