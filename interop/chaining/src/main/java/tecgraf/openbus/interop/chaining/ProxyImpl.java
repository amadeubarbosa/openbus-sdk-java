package tecgraf.openbus.interop.chaining;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.INTERNAL;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OfferRegistry;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.exception.InvalidEncodedStream;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.utils.LibUtils;

public class ProxyImpl extends HelloProxyPOA {

  private static final Logger logger = Logger.getLogger(ProxyImpl.class
    .getName());
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
      String err = "Cadeia em formato inválido";
      logger.log(Level.SEVERE, err, e);
      throw new IllegalArgumentException(err, e);
    }
    context.joinChain(chain);

    ArrayListMultimap<String, String> props = ArrayListMultimap.create();
    props.put("offer.domain", "Interoperability Tests");
    props.put("openbus.component.interface", HelloHelper.id());
    props.put("openbus.component.name", "RestrictedHello");
    List<RemoteOffer> descs;
    try {
      OfferRegistry offers = context.getCurrentConnection().offerRegistry();
      descs = LibUtils.findOffer(offers, props, 1, 10, 1);
    }
    catch (ServiceFailure e) {
      String err = "ServiceFailure ao realizar busca";
      logger.log(Level.SEVERE, err, e);
      throw new INTERNAL(err);
    }

    for (RemoteOffer offer : descs) {
      org.omg.CORBA.Object helloObj =
        offer.service().getFacet(HelloHelper.id());
      if (helloObj == null) {
        continue;
      }

      Hello hello = HelloHelper.narrow(helloObj);
      if (hello == null) {
        continue;
      }
      return hello.sayHello();
    }

    return "";
  }
}
