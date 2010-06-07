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
  @Override
  public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
    Openbus bus = Openbus.getInstance();

    String repID = ri.target_most_derived_interface();
    String method = ri.operation();

    /*
     * Work around para o LocateRequest
     * 
     * Durante o bind com o servidor, o cliente Orbix envia uma mensagem GIOP
     * 1.2 LocateRequest para o servidor, que é uma otimização corba para
     * localizar o objeto. Esta mensageme não passa pelo nosso interceptador
     * cliente e portanto a mensagem é envidada sem a credencial. O JacORB sabe
     * lidar com essa mensagen GIOP, porém diferentemente do Orbix, ele passa
     * essa mensagem pelo interceptador do servidor, que por sua vez faz uma
     * verificação que falha por falta de credencial. Essa mensagem não deve ser
     * verificada.
     * 
     * Analisando o código do JacORB, podemos ver que para uso interno, ele
     * define esse request como uma operação de nome "_non_existent". Então no
     * interceptador do servidor JacORB nós podemos ver esse request com a
     * operação com esse nome.
     * 
     * Logo para podermos responder adequadamente com um GIOP 1.2 LocateReply,
     * foi adicionado uma condição que inibe a verificação no caso de ser essa
     * operação interceptada.
     */
    if (method.equals("_non_existent"))
      return;

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
