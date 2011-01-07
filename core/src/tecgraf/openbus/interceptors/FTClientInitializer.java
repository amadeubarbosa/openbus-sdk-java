/*
 * $Id: TClientInitializer.java
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Essa classe é responsável pelo procedimento de inicialização do cliente
 * associada à inicialização do ORB com mecanismo de tolerancia a falhas
 * ativado.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class FTClientInitializer extends LocalObject implements ORBInitializer {
  /**
   * {@inheritDoc}
   */
  public void post_init(ORBInitInfo info) {
    Logger logger = LoggerFactory.getLogger(ClientInitializer.class);
    try {
      info.add_client_request_interceptor(new FTClientInterceptor(CodecFactory
        .createCodec(info)));
      logger.info("O interceptador cliente tolerante a falhas foi registrado.");
    }
    catch (UserException e) {
      logger.error(
        "Falha ao registrar o interceptador cliente tolerante a falhas.", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void pre_init(ORBInitInfo info) {
    // Nada a ser feito.
  }
}
