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
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Implementa um interceptador "servidor", para obten��o de informa��es no
 * contexto de uma requisi��o.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServerInterceptor extends InterceptorImpl implements
  ServerRequestInterceptor {
  /**
   * Constr�i o interceptador.
   * 
   * @param codec codificador/decodificador
   */
  public ServerInterceptor(Codec codec) {
    super("ServerInterceptor", codec);
  }

  /**
   * {@inheritDoc} <br>
   * Intercepta o request para obten��o de informa��o de contexto.
   */
  public void receive_request_service_contexts(ServerRequestInfo ri) {
    Log.INTERCEPTORS.fine("ATINGI PONTO DE INTERCEPTA��O SERVIDOR!");

    ServiceContext serviceContext;
    try {
      /* Verifica se h� informa��o de contexto (credencial) */
      serviceContext = ri.get_request_service_context(CONTEXT_ID);
      Log.INTERCEPTORS.fine("TEM CREDENCIAL!");
      byte[] value = serviceContext.context_data;
      Credential credential =
        CredentialHelper.extract(this.getCodec().decode_value(value,
          CredentialHelper.type()));
      Log.INTERCEPTORS.fine("CREDENCIAL: " + credential.identifier + ","
        + credential.entityName);

      /* Verifica se a credencial � v�lida */
      CredentialManager credentialManager = CredentialManager.getInstance();
      if (credentialManager.getACS().isValid(credential)) {
        Log.INTERCEPTORS.fine("CREDENCIAL VALIDADA!");

        /*
         * Insere o valor da credencial no slot alocado para seu transporte ao
         * tratador da requisi��o de servi�o
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
      Log.INTERCEPTORS.severe("Falha na valida��o da credencial", e);
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
}
