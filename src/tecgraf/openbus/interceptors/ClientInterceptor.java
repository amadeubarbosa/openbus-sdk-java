/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.util.Log;

/**
 * Implementa um interceptador "cliente", para inserção de informações no
 * contexto de uma requisição.
 * 
 * @author Tecgraf/PUC-Rio
 */
class ClientInterceptor extends InterceptorImpl implements
  ClientRequestInterceptor {

  /**
   * Instância do ORB associado a este interceptador.
   */
  private ORB orb;

  /**
   * Instância do barramento associado a este ORB e interceptador.
   */
  private Openbus bus;

  /**
   * Credencial a ser enviada.
   */
  private Credential credential;

  /**
   * Constrói o interceptador.
   * 
   * @param codec codificador/decodificador
   */
  ClientInterceptor(Codec codec) {
    super("ClientInterceptor", codec);
  }

  /**
   * {@inheritDoc} <br>
   * Intercepta o request para inserção de informação de contexto.
   */
  public void send_request(ClientRequestInfo ri) {
    Log.INTERCEPTORS.info("ATINGI PONTO DE INTERCEPTAÇÃO CLIENTE!");

    /* Verifica se já obteve o barramento */
    if (bus == null) {
      bus = Openbus.getInstance();
      orb = bus.getORB();
    }

    /* Verifica se existe uma credencial para envio */
    credential = bus.getCredential();
    if ((credential == null) || (credential.identifier.equals(""))) {
      Log.INTERCEPTORS.info("SEM CREDENCIAL!");
      return;
    }

    Log.INTERCEPTORS.fine("TEM CREDENCIAL!");

    /* Insere a credencial no contexto do serviço */
    byte[] value = null;
    try {
      Any credentialValue = orb.create_any();
      CredentialHelper.insert(credentialValue, credential);
      value = this.getCodec().encode_value(credentialValue);
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
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void receive_reply(ClientRequestInfo ri) {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void receive_exception(ClientRequestInfo ri) {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void receive_other(ClientRequestInfo ri) {
    // Nada a ser feito.
  }
}
