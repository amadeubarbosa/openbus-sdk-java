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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.core.v1_06.access_control_service.Credential;
import tecgraf.openbus.core.v1_06.access_control_service.IAccessControlService;

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
  public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
    Logger logger = LoggerFactory.getLogger(ServerInterceptor.class);

    String interceptedServant = ri.target_most_derived_interface();
    String interceptedOperation = ri.operation();

    Openbus bus = Openbus.getInstance();
    if (!bus.isInterceptable(interceptedServant, interceptedOperation)) {
      return;
    }

    Credential interceptedCredential = bus.getInterceptedCredential();
    CredentialWrapper wrapper = new CredentialWrapper(interceptedCredential);
    if (interceptedCredential == null) {
      throw new NO_PERMISSION(100, CompletionStatus.COMPLETED_NO);
    }

    IAccessControlService acs = bus.getAccessControlService();
    boolean isValid;
    try {
      isValid = acs.isValid(interceptedCredential);
    }
    catch (SystemException e) {
      logger.error("Erro ao tentar validar a credencial " + wrapper + ".", e);
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }

    if (isValid) {
      logger.info("A credencial {} � v�lida.", wrapper);
    }
    else {
      logger.warn("A credencial {} n�o � v�lida.", wrapper);
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
