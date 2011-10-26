/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import java.lang.reflect.Method;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.CredentialHelper;

/**
 * Implementa um interceptador "cliente", para inserção de informações no
 * contexto de uma requisição.
 * 
 * @author Tecgraf/PUC-Rio
 */
class ClientInterceptor extends InterceptorImpl implements
  ClientRequestInterceptor {

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
    Logger logger = LoggerFactory.getLogger(ClientInterceptor.class);

    logger.info("A operação {} foi interceptada no cliente.", ri.operation());

    Openbus bus = Openbus.getInstance();

    /* Verifica se existe uma credencial para envio */
    Credential credential = bus.getCredential();
    if ((credential == null) || (credential.identifier.equals(""))) {
      logger.info("O cliente não possui credencial para envio.");
      return;
    }

    CredentialWrapper wrapper = new CredentialWrapper(credential);

    /* Insere a credencial no contexto do serviço */
    byte[] value = null;
    try {
      ORB orb = bus.getORB();
      Any credentialValue = orb.create_any();
      CredentialHelper.insert(credentialValue, credential);
      value = this.getCodec().encode_value(credentialValue);
    }
    catch (Exception e) {
      logger.error("Erro na codificação da credencial", e);
      return;
    }
    ri
      .add_request_service_context(new ServiceContext(CONTEXT_ID, value), false);

    logger.debug("A credencial {} foi enviada para a operação {}",
      new Object[] { wrapper, ri.operation() });
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
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void receive_other(ClientRequestInfo ri) {
    // Nada a ser feito.
  }
}
