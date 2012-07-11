package tecgraf.openbus.exception;

/**
 * Exceção de processo de login inválido.
 * 
 * @author Tecgraf
 */
public class InvalidLoginProcess extends OpenBusException {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  public InvalidLoginProcess(String message) {
    super(message);
  }

}
