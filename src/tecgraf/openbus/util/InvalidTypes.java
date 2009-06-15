/*
 * $Id$
 */
package tecgraf.openbus.util;

import openbusidl.acs.Credential;
import scs.core.ComponentId;

/**
 * Representa objetos inv�lidos, usados para indicar erros, de alguns tipos
 * definidos na IDL.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class InvalidTypes {
  /**
   * Representa um identificador de componente inv�lido.
   */
  public static final ComponentId COMPONENT_ID =
    new ComponentId("", (byte) -1, (byte) -1, (byte) -1, "");
  /**
   * Representa uma credencial inv�lida.
   */
  public static final Credential CREDENTIAL = new Credential("", "", "");
}
