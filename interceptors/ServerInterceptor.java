/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.CredentialManager;
import openbus.common.Log;
import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;

import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Implementa um interceptador "servidor", para obtenção de informações no
 * contexto de uma requisição.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServerInterceptor extends org.omg.CORBA.LocalObject implements
  ServerRequestInterceptor {

  /**
   * Representa a identificação do "service context" (contexto) utilizado para
   * transporte de credenciais em requisições de serviço.
   */
  private int contextId;

  /**
   * Representa o objeto responsável pelo marshall/unmarshall de credenciais
   * para transporte/obtenção de contextos de requisições de servico.
   */
  private Codec codec;

  /**
   * Constrói o interceptador.
   * 
   * @param codec codificador/decodificador
   */
  public ServerInterceptor(Codec codec) {
    this.contextId = 1234;
    this.codec = codec;
  }

  /**
   * {@inheritDoc}
   */
  public String name() {
    return "ServerInterceptor";
  }

  /**
   * {@inheritDoc} <br>
   * Intercepta o request para obtenção de informação de contexto.
   */
  public void receive_request_service_contexts(ServerRequestInfo ri) {
    Log.INTERCEPTORS.fine("ATINGI PONTO DE INTERCEPTAÇÂO SERVIDOR!");

    ServiceContext serviceContext;
    try {
      /* Verifica se há informação de contexto (credencial) */
      serviceContext = ri.get_request_service_context(contextId);
      Log.INTERCEPTORS.fine("TEM CREDENCIAL!");
      byte[] value = serviceContext.context_data;
      Credential credential = CredentialHelper.extract(codec.decode_value(
        value, CredentialHelper.type()));
      Log.INTERCEPTORS.fine("CREDENCIAL: " + credential.identifier + ","
        + credential.entityName);

      /* Verifica se a credencial é válida */
      CredentialManager credentialManager = CredentialManager.getInstance();
      if (credentialManager.getACS().isValid(credential)) {
        Log.INTERCEPTORS.fine("CREDENCIAL VALIDADA!");

        /*
         * Insere o valor da credencial no slot alocado para seu transporte ao
         * tratador da requisição de serviço
         */
        ri.set_slot(credentialManager.getCredentialSlot(), credentialManager
          .getCredentialValue(credential));
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
  }

  /**
   * {@inheritDoc}
   */
  public void send_reply(ServerRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void send_exception(ServerRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void send_other(ServerRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void destroy() {
  }
}
