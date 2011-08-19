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

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.CredentialHelper;
import tecgraf.openbus.util.Log;
import tecgraf.openbus.util.TestLogger;

/**
 * Implementa um interceptador "servidor", para obtenção de informações no
 * contexto de uma requisãoo.
 * 
 * @author Tecgraf/PUC-Rio
 */



class ServerInterceptor extends InterceptorImpl implements
  ServerRequestInterceptor {
  /**
   * O slot para transporte da credencial.
   */
  private int credentialSlot;
  public TestLogger log = TestLogger.getInstance();
  ThreadLocal<Long> requestStart = new ThreadLocal<Long>();
  ThreadLocal<String> requestid = new ThreadLocal<String>();
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
    String interceptedOperation = ri.operation();
    Log.INTERCEPTORS.info(String.format(
      "Operação {%s} interceptada no servidor.", interceptedOperation));
    requestStart.set(System.nanoTime());
   
    requestid.set(String.format("%s-%f", interceptedOperation,new Double(System.nanoTime())/10000));
    log.write("receiverequest", "inicio; 0; openbus; " + requestid.get());
    Openbus bus = Openbus.getInstance();
    bus.setInterceptedCredentialSlot(credentialSlot);

    ServiceContext serviceContext = null;
    try {
      serviceContext = ri.get_request_service_context(CONTEXT_ID);
    }
    catch (BAD_PARAM e) {
      Log.INTERCEPTORS.info(String.format(
        "Operação '%s' não possui credencial", interceptedOperation));
      log.write("receiverequest", "fim;" + (System.nanoTime() - requestStart.get()) + 
        "; openbus; " + requestid.get());
      return;
    }

    if (serviceContext == null) {
      Log.INTERCEPTORS
        .severe("Não há informação de contexto (transporte de credencial)");
      log.write("receiverequest", "fim;" + (System.nanoTime() - requestStart.get()) + 
        "; openbus; " + requestid.get());
      return;
    }
    try {
      byte[] value = serviceContext.context_data;
      Credential credential =
        CredentialHelper.extract(this.getCodec().decode_value(value,
          CredentialHelper.type()));
      CredentialWrapper wrapper = new CredentialWrapper(credential);
      Log.INTERCEPTORS.info("Credencial: " + wrapper);

      /*
       * Insere o valor da credencial no slot alocado para seu transporte ao
       * tratador da requisão de serviço
       */
      ORB orb = bus.getORB();
      Any credentialValue = orb.create_any();
      CredentialHelper.insert(credentialValue, credential);
      ri.set_slot(this.credentialSlot, credentialValue);
    }
    catch (Exception e) {
      Log.INTERCEPTORS.severe("Falha na validação da credencial", e);
    }
    log.write("receiverequest", "fim;" + (System.nanoTime() - requestStart.get()) + 
      "; openbus; " + requestid.get());
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
    log.write("sendreply", "fim;" + 0 + 
      "; openbus; " + requestid.get());
    log.write("sendreply", "temporesposta;" + (System.nanoTime() - requestStart.get()) + 
      "; openbus; " + requestid.get());

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
