package tecgraf.openbus.exception;

/**
 * Exce��o que indica que o par host/porta utilizado na cria��o da conex�o n�o
 * apontam para um barramento v�lido ou ativo.
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
