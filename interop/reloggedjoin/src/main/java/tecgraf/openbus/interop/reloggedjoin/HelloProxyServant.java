package tecgraf.openbus.interop.reloggedjoin;

import java.security.interfaces.RSAPrivateKey;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.simple.HelloPOA;
import tecgraf.openbus.utils.LibUtils;

/**
 * Implementação do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloProxyServant extends HelloPOA {
  /** Contexto do OpenBus em uso. */
  private OpenBusContext context;
  /** Chave privada de autenticação */
  private RSAPrivateKey privateKey;
  /** Nome da entidade. */
  private String entity;

  private static final Logger logger = Logger.getLogger(Proxy.class.getName());

  /**
   * Construtor.
   * 
   * @param context Conexão com o barramento.
   * @param privateKey
   * @param entity
   */
  public HelloProxyServant(OpenBusContext context, String entity,
    RSAPrivateKey privateKey) {
    this.context = context;
    this.entity = entity;
    this.privateKey = privateKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String sayHello() {
    try {
      Connection conn = context.getCurrentConnection();
      conn.logout();
      conn.loginByPrivateKey(entity, privateKey);
      CallerChain callerChain = context.getCallerChain();
      String entity = callerChain.caller().entity;
      logger.fine("Chamada recebida de: " + entity);

      context.joinChain(callerChain);
      ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
      properties.put("reloggedjoin.role", "server");
      List<RemoteOffer> services =
        LibUtils.findOffer(conn.offerRegistry(), properties, 1, 10, 1);
      RemoteOffer offer = services.get(0);
      String found = offer.properties(false).get("openbus.offer.entity")
        .get(0);
      logger.fine("serviço da entidade encontrado: " + found);
      org.omg.CORBA.Object helloObj =
        offer.service_ref().getFacetByName("Hello");
      Hello hello = HelloHelper.narrow(helloObj);
      return hello.sayHello();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "something didn't go as expected!";
  }
}
