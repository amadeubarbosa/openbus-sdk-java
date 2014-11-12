package tecgraf.openbus.interop.chaining;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.TRANSIENT;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.InvalidEncodedStream;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;

public class ProxyImpl extends HelloProxyPOA {

  private OpenBusContext context;

  public ProxyImpl(OpenBusContext context) {
    this.context = context;
  }

  @Override
  public String fetchHello(byte[] encodedChain) {
    CallerChain chain;
    try {
      chain = context.decodeChain(encodedChain);
    }
    catch (InvalidEncodedStream e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Cadeia em formato inválido", e);
    }
    context.joinChain(chain);

    ServiceProperty[] properties =
      new ServiceProperty[] {
          new ServiceProperty("offer.domain", "Interoperability Tests"),
          new ServiceProperty("openbus.component.interface", HelloHelper.id()),
          new ServiceProperty("openbus.component.name", "RestrictedHello") };
    ServiceOfferDesc[] descs;
    try {
      OfferRegistry offers = context.getOfferRegistry();
      descs = offers.findServices(properties);
    }
    catch (ServiceFailure e) {
      String err = "ServiceFailure ao realizar busca";
      System.err.println(err);
      throw new INTERNAL(err);
    }
    if (descs.length <= 0) {
      String err = "oferta não encontrada";
      System.err.println(err);
      throw new INTERNAL(err);
    }

    for (ServiceOfferDesc desc : descs) {
      try {
        if (desc.service_ref._non_existent()) {
          continue;
        }
      }
      catch (TRANSIENT e) {
        continue;
      }
      catch (COMM_FAILURE e) {
        continue;
      }
      org.omg.CORBA.Object helloObj =
        desc.service_ref.getFacet(HelloHelper.id());
      if (helloObj == null) {
        System.out
          .println("Não foi possível encontrar uma faceta com esse nome.");
        continue;
      }

      Hello hello = HelloHelper.narrow(helloObj);
      if (hello == null) {
        System.out.println("Faceta encontrada não implementa Hello.");
        continue;
      }
      return hello.sayHello();
    }

    return "";
  }
}
