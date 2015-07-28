package tecgraf.openbus.interop.reloggedjoin;

import java.util.List;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
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

  private static final Logger logger = Logger.getLogger(Client.class.getName());

  /**
   * Fun��o principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.busref;
    String entity = "interop_reloggedjoin_java_client";
    String domain = configs.domain;
    Utils.setTestLogLevel(configs.testlog);
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();
    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.connectByReference(busref);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET),
      domain);

    ServiceProperty[] serviceProperties =
      new ServiceProperty[] {
          new ServiceProperty("reloggedjoin.role", "proxy"),
          new ServiceProperty("offer.domain", "Interoperability Tests") };
    List<ServiceOfferDesc> services =
      LibUtils.findOffer(context.getOfferRegistry(), serviceProperties, 1, 10,
        1);

    if (services.size() > 1) {
      logger.fine("Foram encontrados v�rios proxies do demo Hello: "
        + services.size());
    }

    for (ServiceOfferDesc offerDesc : services) {
      String found =
        LibUtils.findProperty(offerDesc.properties, "openbus.offer.entity");
      logger.fine("Entidade encontrada: " + found);
      org.omg.CORBA.Object helloObj =
        offerDesc.service_ref.getFacetByName("Hello");
      if (helloObj == null) {
        logger.fine("N�o foi poss�vel encontrar uma faceta com esse nome.");
        continue;
      }

      Hello hello = HelloHelper.narrow(helloObj);
      if (hello == null) {
        logger.fine("Faceta encontrada n�o implementa Hello.");
        continue;
      }
      String expected = "Hello " + entity + "!";
      String sayHello = hello.sayHello();
      assert expected.equals(sayHello) : sayHello;
    }
    connection.logout();
  }
}
