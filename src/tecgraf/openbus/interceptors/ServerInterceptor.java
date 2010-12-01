/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.CredentialHelper;

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
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request_service_contexts(ServerRequestInfo ri) {
    Logger logger = LoggerFactory.getLogger(ServerInterceptor.class);

    String interceptedOperation = ri.operation();

    logger.info("A operação {} foi interceptada no servidor.",
      interceptedOperation);
    Openbus bus = Openbus.getInstance();

    bus.setInterceptedCredentialSlot(credentialSlot);

    ServiceContext serviceContext = null;
    try {
      serviceContext = ri.get_request_service_context(CONTEXT_ID);
    }
    catch (BAD_PARAM e) {
      logger.warn("A chamada à operação '{}' não possui credencial.",
        interceptedOperation);
      return;
    }
    if (serviceContext == null) {
      logger.error("A chamada à operação '{}' não possui credencial.",
        interceptedOperation);
      return;
    }

    try {
      byte[] value = serviceContext.context_data;
      Credential credential =
        CredentialHelper.extract(this.getCodec().decode_value(value,
          CredentialHelper.type()));
      CredentialWrapper wrapper = new CredentialWrapper(credential);
      logger.debug("A credencial {} foi interceptada para a operação {}",
        new Object[] { wrapper, interceptedOperation });

      /*
       * Insere o valor da credencial no slot alocado para seu transporte ao
       * tratador da requisição de serviço
       */
      ORB orb = bus.getORB();
      Any credentialValue = orb.create_any();
      CredentialHelper.insert(credentialValue, credential);
      ri.set_slot(this.credentialSlot, credentialValue);
    }
    catch (Exception e) {
      logger.error("Falha na validação da credencial", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request(ServerRequestInfo ri) {
    Logger logger = LoggerFactory.getLogger(ServerInterceptor.class);

    Openbus bus = Openbus.getInstance();

    String interceptedServant = ri.target_most_derived_interface();
    String interceptedOperation = ri.operation();

    if (!bus.isInterceptable(interceptedOperation, interceptedServant)) {
      logger.info("A operação {} não deve ser interceptada pelo servidor.",
        interceptedOperation);
    }
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
