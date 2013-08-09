package tecgraf.openbus.interop.reloggedjoin;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.simple.HelloPOA;
import tecgraf.openbus.interop.util.Utils;

/**
 * Implementação do componente Hello
 * 
 * @author Tecgraf
 */
public final class HelloProxyServant extends HelloPOA {
  /**
   * Contexto do OpenBus em uso.
   */
  private OpenBusContext context;
  private OpenBusPrivateKey privateKey;
  private String entity;

  /**
   * Construtor.
   * 
   * @param context Conexão com o barramento.
   * @param privateKey
   * @param entity
   */
  public HelloProxyServant(OpenBusContext context, String entity,
    OpenBusPrivateKey privateKey) {
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
      System.out.println("relogando entidade: " + entity);
      conn.logout();
      conn.loginByCertificate(entity, privateKey);
      CallerChain callerChain = context.getCallerChain();

      String entity = callerChain.caller().entity;
      System.out.println("Chamada recebida de: " + entity);

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] = new ServiceProperty("reloggedjoin.role", "server");

      context.joinChain(callerChain);

      System.out.println("buscando serviço hello");
      ServiceOfferDesc[] services =
        context.getOfferRegistry().findServices(serviceProperties);
      for (ServiceOfferDesc offerDesc : services) {
        String found =
          Utils.findProperty(offerDesc.properties, "openbus.offer.entity");
        System.out.println("serviço da entidade encontrado: " + found);
        org.omg.CORBA.Object helloObj =
          offerDesc.service_ref.getFacetByName("Hello");
        Hello hello = HelloHelper.narrow(helloObj);
        return hello.sayHello();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "no service found!";
  }
}
