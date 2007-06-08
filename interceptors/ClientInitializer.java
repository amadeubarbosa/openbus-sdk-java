/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.Log;

import org.omg.CORBA.UserException;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.Encoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;

/**
 * Essa classe � respons�vel pelo procedimento de inicializa��o do cliente
 * associada � inicializa��o do ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ClientInitializer extends org.omg.CORBA.LocalObject implements
  ORBInitializer {

  /**
   * Constr�i o inicializador do cliente
   */
  public ClientInitializer() {
  }

  /**
   * {@inheritDoc} <br>
   * 
   * Registra o interceptador de requisi��es de servi�o do cliente
   */
  public void post_init(ORBInitInfo info) {
    try {
      CodecFactory codecFactory = CodecFactoryHelper.narrow(info
        .resolve_initial_references("CodecFactory"));
      Encoding encoding = new Encoding((short) 0, (byte) 1, (byte) 2);
      Codec codec = codecFactory.create_codec(encoding);

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
