/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.CredentialHelper;
import tecgraf.openbus.util.Log;
import tecgraf.openbus.util.TestLogger;

/**
 * Implementa um interceptador "cliente", para inserção de informações no
 * contexto de uma requisição.
 * 
 * @author Tecgraf/PUC-Rio
 */
class ClientInterceptor extends InterceptorImpl implements
  ClientRequestInterceptor {
  
   TestLogger log = TestLogger.getInstance();
   ThreadLocal<Long> requestStart = new ThreadLocal<Long>();
   ThreadLocal<String> requestid = new ThreadLocal<String>();

  
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
    Log.INTERCEPTORS.info("Operação {" + ri.operation()
      + "} interceptada no cliente.");
    long sReqInit = System.nanoTime();
    requestStart.set(sReqInit);
    requestid.set(String.format("%s-%f", ri.operation(),new Double(System.nanoTime())/10000));
    //operacoes do ORB nao precisam de credencial
    for (java.lang.reflect.Method op : ClientInterceptor.class.getMethods()) {
      if (ri.operation().equals(op.getName()) ) return;
    } 
    
    Openbus bus = Openbus.getInstance();

    /* Verifica se existe uma credencial para envio */
    Credential credential = bus.getCredential();
    if ((credential == null) || (credential.identifier.equals(""))) {
      Log.INTERCEPTORS.info("Operação {" + ri.operation()
      + "} SEM CREDENCIAL!");
      return;
    }

    CredentialWrapper wrapper = new CredentialWrapper(credential);
    Log.INTERCEPTORS.info("Operação {" + ri.operation()
    	      + "} Credencial: " + wrapper);

    /* Insere a credencial no contexto do serviço */
    byte[] value = null;
    try {
      ORB orb = bus.getORB();
      Any credentialValue = orb.create_any();
      CredentialHelper.insert(credentialValue, credential);
      value = this.getCodec().encode_value(credentialValue);
    }
    catch (Exception e) {
      Log.INTERCEPTORS.severe("Operação {" + ri.operation()
    	      + "} ERRO NA CODIFICAÇÂO DA CREDENCIAL!", e);
      return;
    }
    ri
      .add_request_service_context(new ServiceContext(CONTEXT_ID, value), false);
    log.write("sendrequest", "fim; "+ (System.nanoTime() - sReqInit) +
              "; openbus; " + requestid.get());
    Log.INTERCEPTORS.fine("Operação {" + ri.operation()
      + "} INSERI CREDENCIAL!");
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
    log.write("receivereply", "fim;" + 0 + 
      "; openbus; " + requestid.get());
    log.write("receivereply", "total;" + (System.nanoTime() - requestStart.get()) + 
      "; openbus; " + requestid.get());
  }

  /**
   * {@inheritDoc}
   * 
   * @throws ForwardRequest
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
