/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.util.Log;

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
      Openbus bus = Openbus.getInstance();
      CredentialValidationPolicy policy = bus.getCredentialValidationPolicy();
      switch (policy) {
        case ALWAYS:
          info
            .add_server_request_interceptor(CredentialValidatorServerInterceptor
              .getInstance());
          break;
        case CACHED:
          info
            .add_server_request_interceptor(CachedCredentialValidatorServerInterceptor
              .getInstance());
          break;
        case NONE:
          break;
        default:
          Log.INTERCEPTORS
            .warning("Não foi escolhida nenhuma política para a validação de credenciais obtidas pelo interceptador servidor.");
          break;
      }
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
