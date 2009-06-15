package tecgraf.openbus.exception;

/**
 * Representa uma exceção de falha ao realizar a autenticação no serviço de
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
