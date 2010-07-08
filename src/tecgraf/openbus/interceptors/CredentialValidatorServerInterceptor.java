/*
 * $Id $
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.util.Log;

/**
 * Implementa a política de validação de credenciais interceptadas em um
 * servidor que verifica a validade de uma credencial em toda interceptação.
 * 
 * @author Tecgraf/PUC-Rio
 */
final class CredentialValidatorServerInterceptor extends LocalObject implements
  ServerRequestInterceptor {
  /**
   * A instância única do interceptador.
   */
  private static CredentialValidatorServerInterceptor instance;

  /**
   * Cria o interceptador para validação de credenciais.
   */
  private CredentialValidatorServerInterceptor() {
  }

  /**
   * Obtém a instância única do interceptador.
   * 
   * @return A instância única do interceptador.
   */
  public static CredentialValidatorServerInterceptor getInstance() {
    if (instance == null) {
      instance = new CredentialValidatorServerInterceptor();
    }
    return instance;
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
    Openbus bus = Openbus.getInstance();

    String repID = ri.target_most_derived_interface();
    String method = ri.operation();

    boolean isInterceptable = bus.isInterceptable(repID, method);
    if (!isInterceptable) {
      Log.INTERCEPTORS.info(String.format(
        "Operação '%s' não interceptada no servidor.", method));
      return;
    }

    Credential interceptedCredential = bus.getInterceptedCredential();
    if (interceptedCredential == null) {
      Log.INTERCEPTORS.warning("Nenhuma credencial foi interceptada.");
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }

    CredentialWrapper wrapper = new CredentialWrapper(interceptedCredential);
    Log.INTERCEPTORS.info("Credencial interceptada: " + wrapper);

    IAccessControlService acs = bus.getAccessControlService();
    boolean isValid;
    try {
      isValid = acs.isValid(interceptedCredential);
    }
    catch (SystemException e) {
      Log.INTERCEPTORS.severe("Erro ao tentar validar uma credencial.", e);
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }

    if (isValid) {
      Log.INTERCEPTORS.info("A credencial interceptada " + wrapper
        + " é válida.");
    }
    else {
      Log.INTERCEPTORS.warning("A credencial interceptada " + wrapper
        + " não é válida.");
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request_service_contexts(ServerRequestInfo ri)
    throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_other(ServerRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_reply(ServerRequestInfo ri) {
    // Nada a ser feito.

  }

  /**
   * {@inheritDoc}
   */
  public void destroy() {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public String name() {
    return CredentialValidatorServerInterceptor.class.getName();
  }

}
