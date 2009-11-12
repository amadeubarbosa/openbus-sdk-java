/*
 * $Id$
 */
package tecgraf.openbus.authenticators;

import openbusidl.acs.Credential;
import openbusidl.acs.IAccessControlService;
import tecgraf.openbus.exception.OpenBusException;

/**
 * Utilizado para efetuar a autenticação de uma entidade junto ao Serviço de
 * Controle de Acesso.
 * 
 * @author Tecgraf/PUC-Rio
 */
public interface Authenticator {
  /**
   * Autentica uma entidade no barramento.
   * 
   * @param acs O serviço de controle de acesso.
   * 
   * @return A credencial da entidade, ou {@code null}, caso a entidade não
   *         tenha permissão de acesso.
   * 
   * @throws OpenBusException Caso a autenticação falhe.
   */
  Credential authenticate(IAccessControlService acs) throws OpenBusException;
}
