/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tecgraf.openbus.Openbus;

/**
 * Essa classe � respons�vel pelo procedimento de inicializa��o do servidor
 * associada � inicializa��o do ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServerInitializer extends LocalObject implements ORBInitializer {
  /**
   * {@inheritDoc} <br>
   * Registra o interceptador de requisi��es de servi�o do servidor
   */
  public void post_init(ORBInitInfo info) {
    Logger logger = LoggerFactory.getLogger(ServerInitializer.class);
    try {
      info.add_server_request_interceptor(new ServerInterceptor(CodecFactory
        .createCodec(info), info.allocate_slot_id()));
      logger.info("O interceptador servidor foi registrado.");
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
          logger
            .error("N�o foi escolhida nenhuma pol�tica para a valida��o de credenciais obtidas pelo interceptador servidor.");
          break;
      }

    }
    catch (UserException e) {
      logger.error("Falha ao registrar o interceptador servidor.", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void pre_init(ORBInitInfo info) {
    // Nada a ser feito.
  }
}