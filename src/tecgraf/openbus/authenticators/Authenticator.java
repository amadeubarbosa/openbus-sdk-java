/*
 * $Id$
 */
package tecgraf.openbus.authenticators;

import openbusidl.acs.Credential;
import openbusidl.acs.IAccessControlService;
import tecgraf.openbus.exception.OpenBusException;

/**
 * Utilizado para efetuar a autentica��o de uma entidade junto ao Servi�o de
 * Controle de Acesso.
 * 
 * @author Tecgraf/PUC-Rio
 */
public interface Authenticator {
  /**
   * Autentica uma entidade no barramento.
   * 
   * @param acs O servi�o de controle de acesso.
   * 
   * @return A credencial da entidade, ou {@code null}, caso a entidade n�o
   *         tenha permiss�o de acesso.
   * 
   * @throws OpenBusException Caso a autentica��o falhe.
   */
  Credential authenticate(IAccessControlService acs) throws OpenBusException;
}
