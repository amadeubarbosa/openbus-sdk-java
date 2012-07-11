package tecgraf.openbus.exception;

/**
 * Exce��o indicando chave privada n�o corresponde � chave p�blica ou
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
   * @param exception exce��o original.
   */
  public WrongPrivateKey(String message, Throwable exception) {
    super(message, exception);
  }

}
