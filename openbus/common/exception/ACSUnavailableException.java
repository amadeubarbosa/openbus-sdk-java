/*
 * $Id$
 */
package openbus.common.exception;

/**
 * Representa uma exce��o de servi�o de controle de acesso indispon�vel.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class ACSUnavailableException extends ServiceUnavailableException {
  /**
   * Cria uma exce��o de servi�o de controle de acesso indispon�vel com uma
   * mensagem associada.
   * 
   * @param message A mensagem de erro.
   */
  public ACSUnavailableException(String message) {
    super(message);
  }

  /**
   * Cria uma exce��o de servi�o de controle de acesso indispon�vel com uma
   * mensagem e uma causa associadas.
   * 
   * @param message A mensagem de erro.
   * @param cause A causa.
   */
  public ACSUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }

}
