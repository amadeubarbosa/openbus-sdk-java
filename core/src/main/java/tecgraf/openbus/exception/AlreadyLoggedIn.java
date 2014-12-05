package tecgraf.openbus.exception;

/**
 * Exce��o indicando que j� possui um login v�lido.
 * 
 * @author Tecgraf
 */
public final class AlreadyLoggedIn extends OpenBusException {

  /**
   * Construtor.
   */
  public AlreadyLoggedIn() {
    super("J� existe um login ativo nesta conex�o");
  }
}
