/*
 * $Id$
 */
package tecgraf.openbus.exception;

import java.security.GeneralSecurityException;

/**
 * Representa uma exceção gerada pelos mecanismos de segurança: criptografia e
 * assinatura digital.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class PKIException extends OpenBusException {
  /**
   * Cria a exceção com uma mensagem e uma causa associadas.
   * 
   * @param message A mensagem.
   * @param cause A causa.
   */
  public PKIException(String message, GeneralSecurityException cause) {
    super(message, cause);
  }
}
