/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.Log;
import openbus.interceptors.CodecFactory;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;

/**
 * Essa classe é responsável pelo procedimento de inicialização do cliente
 * associada à inicialização do ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ClientInitializer extends LocalObject implements ORBInitializer {
  /**
   * {@inheritDoc}
   */
  public void post_init(ORBInitInfo info) {
    try {
      info.add_client_request_interceptor(new ClientInterceptor(CodecFactory
        .createCodec(info)));
      Log.INTERCEPTORS.info("REGISTREI INTERCEPTADOR CLIENTE!");
    }
    catch (UserException e) {
      Log.INTERCEPTORS.severe("ERRO NO REGISTRO DO INTERCEPTADOR CLIENTE!", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void pre_init(ORBInitInfo info) {
    // Nada a ser feito.
  }
}
