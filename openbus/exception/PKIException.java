/*
 * $Id$
 */
package openbus.exception;

import java.security.GeneralSecurityException;

import openbus.common.exception.OpenBusException;

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
