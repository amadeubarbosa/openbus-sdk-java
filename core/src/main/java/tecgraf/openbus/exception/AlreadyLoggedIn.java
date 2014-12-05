package tecgraf.openbus.exception;

/**
 * Exceção indicando que já possui um login válido.
 * 
 * @author Tecgraf
 */
public final class AlreadyLoggedIn extends OpenBusException {

  /**
   * Construtor.
   */
  public AlreadyLoggedIn() {
    super("Já existe um login ativo nesta conexão");
  }
}
