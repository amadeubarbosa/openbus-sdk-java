/*
 * $Id$
 */
package openbus.exception;

import openbus.common.exception.OpenBusException;

import org.omg.CORBA.SystemException;
import org.omg.CORBA.UserException;

/**
 * Representa uma exceção gerada pelo <i>runtime</i> de CORBA.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class CORBAException extends OpenBusException {
  /**
   * Cria a exceção com uma causa associada.
   * 
   * @param cause A causa.
   */
  public CORBAException(SystemException cause) {
    super(cause);
  }

  /**
   * Cria a exceção com uma causa associada.
   * 
   * @param cause A causa.
   */
  public CORBAException(UserException cause) {
    super(cause);
  }
}