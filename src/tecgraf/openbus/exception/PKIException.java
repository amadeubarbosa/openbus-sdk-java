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
   * Cria a exceção com uma causa associada.
   * 
   * @param cause A causa.
   */
  public PKIException(GeneralSecurityException cause) {
    super(cause);
  }
}
