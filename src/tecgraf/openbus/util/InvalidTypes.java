/*
 * $Id$
 */
package tecgraf.openbus.util;

import scs.core.ComponentId;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;

/**
 * Representa objetos inválidos, usados para indicar erros, de alguns tipos
 * definidos na IDL.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class InvalidTypes {
  /**
   * Representa um identificador de componente inválido.
   */
  public static final ComponentId COMPONENT_ID =
    new ComponentId("", (byte) -1, (byte) -1, (byte) -1, "");
  /**
   * Representa uma credencial inválida.
   */
  public static final Credential CREDENTIAL = new Credential("", "", "");
}
