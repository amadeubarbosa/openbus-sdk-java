/*
 * $Id $
 */
package tecgraf.openbus.interceptors;

import openbusidl.acs.Credential;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.util.Log;

/**
 * Implementa a pol�tica de valida��o de credenciais interceptadas em um
 * servidor que verifica a validade de uma credencial em toda intercepta��o.
 * 
 * @author Tecgraf/PUC-Rio
 */
final class CredentialValidatorServerInterceptor extends LocalObject implements
  ServerRequestInterceptor {
  /**
   * A inst�ncia �nica do interceptador.
   */
  private static CredentialValidatorServerInterceptor instance;

  /**
   * Cria o interceptador para valida��o de credenciais.
   */
  private CredentialValidatorServerInterceptor() {
  }

  /**
   * Obt�m a inst�ncia �nica do interceptador.
   * 
   * @return A inst�ncia �nica do interceptador.
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
  @Override
  public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
    Openbus bus = Openbus.getInstance();
    Credential interceptedCredential = bus.getInterceptedCredential();
    if (interceptedCredential == null) {
      Log.INTERCEPTORS.info("Nenhuma credencial foi interceptada.");
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
        + " � v�lida.");
    }
    else {
      Log.INTERCEPTORS.info("A credencial interceptada " + wrapper
        + " n�o � v�lida.");
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request_service_contexts(ServerRequestInfo ri)
    throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_other(ServerRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_reply(ServerRequestInfo ri) {
    // Nada a ser feito.

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String name() {
    return CredentialValidatorServerInterceptor.class.getName();
  }

}
