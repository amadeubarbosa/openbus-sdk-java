package tecgraf.openbus.exception;

/**
 * Exceção que indica que o par host/porta utilizado na criação da conexão não
 * apontam para um barramento válido ou ativo.
 * 
 * @author Tecgraf
 */
public class InvalidBusAddress extends OpenBusException {

  /**
   * Construtor.
   * 
   * @param message mensagem de erro.
   */
  public InvalidBusAddress(String message) {
    super(message);
  }

}
