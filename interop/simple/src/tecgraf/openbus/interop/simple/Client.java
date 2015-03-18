package tecgraf.openbus.interop.simple;

import java.util.List;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Configs;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.security.Cryptography;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class Client {
  /**
   * Função principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String host = configs.bushost;
    int port = configs.busport;
    String entity = "interop_hello_java_client";
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.createConnection(host, port);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET));

    ServiceProperty[] serviceProperties = new ServiceProperty[2];
    serviceProperties[0] =
      new ServiceProperty("openbus.component.interface", HelloHelper.id());
    serviceProperties[1] =
      new ServiceProperty("offer.domain", "Interoperability Tests");
    List<ServiceOfferDesc> services =
      Utils.findOffer(context.getOfferRegistry(), serviceProperties, 1, 10, 1);

    for (ServiceOfferDesc offerDesc : services) {
      String found =
        Utils.findProperty(offerDesc.properties, "openbus.offer.entity");
      org.omg.CORBA.Object helloObj =
        offerDesc.service_ref.getFacetByName("Hello");
      if (helloObj == null) {
        continue;
      }

      Hello hello = HelloHelper.narrow(helloObj);
      if (hello == null) {
        continue;
      }
      String expected = "Hello " + entity + "!";
      String sayHello = hello.sayHello();
      assert expected.equals(sayHello) : String.format("Entidade (%s): %s",
        found, sayHello);
    }

    connection.logout();
  }
}
