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
 * Essa classe é responsável pelo procedimento de inicialização do servidor
 * associada à inicialização do ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServerInitializer extends LocalObject implements ORBInitializer {
  /**
   * {@inheritDoc} <br>
   * Registra o interceptador de requisições de serviço do servidor
   */
  public void post_init(ORBInitInfo info) {
    try {
      info.add_server_request_interceptor(new ServerInterceptor(CodecFactory
        .createCodec(info), info.allocate_slot_id()));
      Log.INTERCEPTORS.info("REGISTREI INTERCEPTADOR SERVIDOR!");
    }
    catch (UserException e) {
      Log.INTERCEPTORS.severe("ERRO NO REGISTRO DO INTERCEPTADOR SERVIDOR!", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void pre_init(ORBInitInfo info) {
    // Nada a ser feito.
  }
}
