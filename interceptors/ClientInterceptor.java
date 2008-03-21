/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.CredentialManager;
import openbus.common.Log;

import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;

/**
 * Implementa um interceptador "cliente", para inserção de informações no
 * contexto de uma requisição.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ClientInterceptor extends InterceptorImpl implements
  ClientRequestInterceptor {
  /**
   * Constrói o interceptador.
   * 
   * @param codec codificador/decodificador
   */
  public ClientInterceptor(Codec codec) {
    super("ClientInterceptor", codec);
  }

  /**
   * {@inheritDoc} <br>
   * Intercepta o request para inserção de informação de contexto.
   */
  public void send_request(ClientRequestInfo ri) {
    Log.INTERCEPTORS.info("ATINGI PONTO DE INTERCEPTAÇÂO CLIENTE!");

    /* Verifica se existe uma credencial para envio */
    CredentialManager credentialManager = CredentialManager.getInstance();
    if (!credentialManager.hasMemberCredential()) {
      Log.INTERCEPTORS.info("SEM CREDENCIAL!");
      return;
    }
    Log.INTERCEPTORS.fine("TEM CREDENCIAL!");

    /* Insere a credencial no contexto do serviço */
    byte[] value = null;
    try {
      value =
        this.getCodec().encode_value(
          credentialManager.getMemberCredentialValue());
    }
    catch (Exception e) {
      Log.INTERCEPTORS.severe("ERRO NA CODIFICAÇÂO DA CREDENCIAL!", e);
      return;
    }
    ri
      .add_request_service_context(new ServiceContext(CONTEXT_ID, value), false);
    Log.INTERCEPTORS.fine("INSERI CREDENCIAL!");
  }

  /**
   * {@inheritDoc}
   */
  public void send_poll(ClientRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void receive_reply(ClientRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void receive_exception(ClientRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void receive_other(ClientRequestInfo ri) {
  }
}
