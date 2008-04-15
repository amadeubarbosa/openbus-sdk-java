/*
 * $Id$
 */
package openbus.common.exception;

/**
 * Representa uma exceção de serviço de controle de acesso indisponível.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class ACSUnavailableException extends ServiceUnavailableException {
  /**
   * Cria uma exceção de serviço de controle de acesso indisponível com uma
   * mensagem associada.
   * 
   * @param message A mensagem de erro.
   */
  public ACSUnavailableException(String message) {
    super(message);
  }

  /**
   * Cria uma exceção de serviço de controle de acesso indisponível com uma
   * mensagem e uma causa associadas.
   * 
   * @param message A mensagem de erro.
   * @param cause A causa.
   */
  public ACSUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }

}
