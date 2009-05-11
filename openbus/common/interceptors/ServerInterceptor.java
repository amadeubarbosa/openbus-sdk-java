/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.AccessControlServiceWrapper;
import openbus.Registry;
import openbus.common.Log;
import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Implementa um interceptador "servidor", para obtenção de informações no
 * contexto de uma requisição.
 * 
 * @author Tecgraf/PUC-Rio
 */
class ServerInterceptor extends InterceptorImpl implements
  ServerRequestInterceptor {
  /**
   * O slot para transporte da credencial.
   */
  private int credentialSlot;

  /**
   * Constrói o interceptador.
   * 
   * @param codec codificador/decodificador
   * @param credentialSlot O slot para transporte da credencial.
   */
  ServerInterceptor(Codec codec, int credentialSlot) {
    super("ServerInterceptor", codec);
    this.credentialSlot = credentialSlot;
    Registry.getInstance().setRequestCredentialSlot(this.credentialSlot);
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request_service_contexts(ServerRequestInfo ri) {
    Log.INTERCEPTORS.fine("ATINGI PONTO DE INTERCEPTAÇÂO SERVIDOR!");

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
    if (ri.operation().equals("_non_existent"))
      return;

    ServiceContext serviceContext;
    try {
      /* Verifica se há informação de contexto (credencial) */
      serviceContext = ri.get_request_service_context(CONTEXT_ID);
      Log.INTERCEPTORS.fine("TEM CREDENCIAL!");
      byte[] value = serviceContext.context_data;
      Credential credential =
        CredentialHelper.extract(this.getCodec().decode_value(value,
          CredentialHelper.type()));
      Log.INTERCEPTORS.fine("CREDENCIAL: " + credential.identifier + ","
        + credential.owner);

      AccessControlServiceWrapper acs = Registry.getInstance().getACS();
      /* Verifica se a credencial é válida */
      if (acs.isValid(credential)) {
        Log.INTERCEPTORS.fine("CREDENCIAL VALIDADA!");

        /*
         * Insere o valor da credencial no slot alocado para seu transporte ao
         * tratador da requisição de serviço
         */
        ORB orb = Registry.getInstance().getORBWrapper().getORB();
        Any credentialValue = orb.create_any();
        CredentialHelper.insert(credentialValue, credential);
        ri.set_slot(this.credentialSlot, credentialValue);
      }
      else {
        Log.INTERCEPTORS.info("CREDENCIAL INVALIDA!");
        throw new org.omg.CORBA.NO_PERMISSION(0,
          org.omg.CORBA.CompletionStatus.COMPLETED_NO);
      }
    }
    catch (Exception e) {
      Log.INTERCEPTORS.severe("Falha na validação da credencial", e);
      throw new org.omg.CORBA.NO_PERMISSION(0,
        org.omg.CORBA.CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request(ServerRequestInfo ri) {
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
  public void send_exception(ServerRequestInfo ri) {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_other(ServerRequestInfo ri) {
    // Nada a ser feito.
  }
}
