/*
 * $Id$
 */
package tecgraf.openbus.common.interceptors;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;

import tecgraf.openbus.common.Log;
import tecgraf.openbus.interceptors.CodecFactory;

/**
 * Essa classe � respons�vel pelo procedimento de inicializa��o do cliente
 * associada � inicializa��o do ORB.
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
