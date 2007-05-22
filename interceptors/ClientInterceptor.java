/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.CredentialManager;

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
public class ClientInterceptor extends org.omg.CORBA.LocalObject implements
  ClientRequestInterceptor {
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
   * @param codec
   */
  public ClientInterceptor(Codec codec) {
    this.contextId = 1234;
    this.codec = codec;
  }

  /**
   * {@inheritDoc}
   */
  public String name() {
    return "ClientInterceptor";
  }

  /**
   * {@inheritDoc} <br>
   * Intercepta o request para inserção de informação de contexto.
   */
  public void send_request(ClientRequestInfo ri) {
    System.out.println("ATINGI PONTO DE INTERCEPTAÇÂO CLIENTE!");

    /* Verifica se existe uma credencial para envio */
    CredentialManager credentialManager = CredentialManager.getInstance();
    if (!credentialManager.hasMemberCredential()) {
      System.out.println("SEM CREDENCIAL!");
      return;
    }
    System.out.println("TEM CREDENCIAL!");

    /* Insere a credencial no contexto do serviço */
    byte[] value = null;
    try {
      value = codec.encode_value(credentialManager.getMemberCredentialValue());
    }
    catch (Exception e) {
      System.out.println("ERRO NA CODIFICAÇÂO DA CREDENCIAL!");
      e.printStackTrace();
      return;
    }
    ri.add_request_service_context(new ServiceContext(contextId, value), false);
    System.out.println("INSERI CREDENCIAL!");
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

  /**
   * {@inheritDoc}
   */
  public void destroy() {
  }
}