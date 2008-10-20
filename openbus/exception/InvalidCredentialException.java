/*
 * $Id$
 */
package openbus.exception;

import openbus.common.exception.OpenBusException;

import org.omg.CORBA.NO_PERMISSION;

/**
 * Representa uma exceção de tentativa de execução de uma operação do OpenBus
 * com uma credencial inválida.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class InvalidCredentialException extends OpenBusException {
  /**
   * Cria a exceção com uma causa associada.
   * 
   * @param cause A causa.
   */
  public InvalidCredentialException(NO_PERMISSION cause) {
    super(cause);
  }
}
