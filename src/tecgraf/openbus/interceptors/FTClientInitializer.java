/*
 * $Id: TClientInitializer.java 
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;

import tecgraf.openbus.util.Log;

/**
 * Essa classe é responsável pelo procedimento de inicialização do cliente
 * associada à inicialização do ORB com mecanismo de tolerancia a falhas ativado.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class FTClientInitializer extends LocalObject implements ORBInitializer {
  /**
   * {@inheritDoc}
   */
  public void post_init(ORBInitInfo info) {
    try {
      info.add_client_request_interceptor(new FTClientInterceptor(CodecFactory
        .createCodec(info)));
      Log.INTERCEPTORS.info("REGISTREI INTERCEPTADOR CLIENTE TOLERANTE A FALHAS!");
    }
    catch (UserException e) {
      Log.INTERCEPTORS.severe("ERRO NO REGISTRO DO INTERCEPTADOR CLIENTE TOLERANTE A FALHAS!", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void pre_init(ORBInitInfo info) {
    // Nada a ser feito.
  }
}
