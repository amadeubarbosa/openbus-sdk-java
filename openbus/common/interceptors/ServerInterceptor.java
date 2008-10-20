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
 * Implementa um interceptador "servidor", para obten��o de informa��es no
 * contexto de uma requisi��o.
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
   * Constr�i o interceptador.
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
    Log.INTERCEPTORS.fine("ATINGI PONTO DE INTERCEPTA��O SERVIDOR!");

    ServiceContext serviceContext;
    try {
      /* Verifica se h� informa��o de contexto (credencial) */
      serviceContext = ri.get_request_service_context(CONTEXT_ID);
      Log.INTERCEPTORS.fine("TEM CREDENCIAL!");
      byte[] value = serviceContext.context_data;
      Credential credential = CredentialHelper.extract(this.getCodec()
        .decode_value(value, CredentialHelper.type()));
      Log.INTERCEPTORS.fine("CREDENCIAL: " + credential.identifier + ","
        + credential.entityName);

      AccessControlServiceWrapper acs = Registry.getInstance().getACS();
      /* Verifica se a credencial � v�lida */
      if (acs.isValid(credential)) {
        Log.INTERCEPTORS.fine("CREDENCIAL VALIDADA!");

        /*
         * Insere o valor da credencial no slot alocado para seu transporte ao
         * tratador da requisi��o de servi�o
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
      Log.INTERCEPTORS.severe("Falha na valida��o da credencial", e);
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
