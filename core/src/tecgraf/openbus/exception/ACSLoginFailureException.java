package tecgraf.openbus.exception;

/**
 * Representa uma exce��o de falha ao realizar a autentica��o no servi�o de
 * controle de acesso
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class ACSLoginFailureException extends OpenBusException {
  public ACSLoginFailureException() {
    super();
  }

  public ACSLoginFailureException(String message, Throwable cause) {
    super(message, cause);
  }

  public ACSLoginFailureException(String message) {
    super(message);
  }

}
