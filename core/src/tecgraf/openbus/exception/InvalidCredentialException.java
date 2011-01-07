/*
 * $Id$
 */
package tecgraf.openbus.exception;

import org.omg.CORBA.NO_PERMISSION;

/**
 * Representa uma exce��o de tentativa de execu��o de uma opera��o do OpenBus
 * com uma credencial inv�lida.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class InvalidCredentialException extends OpenBusException {
  /**
   * Cria a exce��o com uma causa associada.
   * 
   * @param cause A causa.
   */
  public InvalidCredentialException(NO_PERMISSION cause) {
    super(cause);
  }
}
