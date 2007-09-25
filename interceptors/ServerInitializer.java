/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.CredentialManager;
import openbus.common.Log;

import org.omg.CORBA.UserException;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ORBInitInfo;

/**
 * Essa classe é responsável pelo procedimento de inicialização do servidor
 * associada à inicialização do ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServerInitializer extends Initializer {
  /**
   * {@inheritDoc} <br>
   * Registra o interceptador de requisições de serviço do servidor
   */
  public void post_init(ORBInitInfo info) {
    try {
      /* Instala o interceptador */
      Codec codec = createCodec(info);
      info.add_server_request_interceptor(new ServerInterceptor(codec));
      Log.INTERCEPTORS.info("REGISTREI INTERCEPTADOR SERVIDOR!");

      /* Aloca um slot para transporte da credencial */
      int credentialSlot = info.allocate_slot_id();
      CredentialManager.getInstance().setCredentialSlot(credentialSlot);
    }
    catch (UserException e) {
      Log.INTERCEPTORS.severe("ERRO NO REGISTRO DO INTERCEPTADOR SERVIDOR!", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void pre_init(ORBInitInfo info) {
  }
}
