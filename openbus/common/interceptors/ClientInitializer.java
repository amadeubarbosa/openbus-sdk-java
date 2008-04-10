/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.Log;

import org.omg.CORBA.UserException;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ORBInitInfo;

/**
 * Essa classe � respons�vel pelo procedimento de inicializa��o do cliente
 * associada � inicializa��o do ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ClientInitializer extends Initializer {
  /**
   * {@inheritDoc} <br>
   * 
   * Registra o interceptador de requisi��es de servi�o do cliente
   */
  public void post_init(ORBInitInfo info) {
    try {
      Codec codec = createCodec(info);
      info.add_client_request_interceptor(new ClientInterceptor(codec));
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
  }
}
