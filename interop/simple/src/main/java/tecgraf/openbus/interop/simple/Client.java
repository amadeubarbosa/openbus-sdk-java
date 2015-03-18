package tecgraf.openbus.interop.simple;

import java.util.List;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.Utils;

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
    String iorfile = configs.busref;
    String entity = "interop_hello_java_client";
    String domain = configs.domain;
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();
    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.connectByReference(busref);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET),
      domain);

    ServiceProperty[] serviceProperties = new ServiceProperty[2];
    serviceProperties[0] =
      new ServiceProperty("openbus.component.interface", HelloHelper.id());
    serviceProperties[1] =
      new ServiceProperty("offer.domain", "Interoperability Tests");
    List<ServiceOfferDesc> services =
      LibUtils.findOffer(context.getOfferRegistry(), serviceProperties, 1, 10,
        1);

    for (ServiceOfferDesc offerDesc : services) {
      String found =
        LibUtils.findProperty(offerDesc.properties, "openbus.offer.entity");
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
    orb.shutdown(true);
    orb.destroy();
  }
}
