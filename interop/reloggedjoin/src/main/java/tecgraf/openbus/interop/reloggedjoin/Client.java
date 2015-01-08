package tecgraf.openbus.interop.reloggedjoin;

import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.security.Cryptography;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class Client {

  private static final Logger logger = Logger.getLogger(Client.class.getName());

  /**
   * Função principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) throws Exception {
    Properties props = Utils.readPropertyFile("/test.properties");
    String host = props.getProperty("bus.host.name");
    int port = Integer.valueOf(props.getProperty("bus.host.port"));
    String entity = "interop_reloggedjoin_java_client";
    String domain = "testing";
    Utils.setTestLogLevel(Level.parse(props.getProperty("log.test", "OFF")));
    Utils.setLibLogLevel(Level.parse(props.getProperty("log.lib", "OFF")));

    ORB orb = ORBInitializer.initORB();
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.connectByAddress(host, port);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET),
      domain);

    ServiceProperty[] serviceProperties =
      new ServiceProperty[] {
          new ServiceProperty("reloggedjoin.role", "proxy"),
          new ServiceProperty("offer.domain", "Interoperability Tests") };
    List<ServiceOfferDesc> services =
      Utils.findOffer(context.getOfferRegistry(), serviceProperties, 1, 10, 1);

    if (services.size() > 1) {
      logger.fine("Foram encontrados vários proxies do demo Hello: "
        + services.size());
    }

    for (ServiceOfferDesc offerDesc : services) {
      String found =
        Utils.findProperty(offerDesc.properties, "openbus.offer.entity");
      logger.fine("Entidade encontrada: " + found);
      org.omg.CORBA.Object helloObj =
        offerDesc.service_ref.getFacetByName("Hello");
      if (helloObj == null) {
        logger.fine("Não foi possível encontrar uma faceta com esse nome.");
        continue;
      }

      Hello hello = HelloHelper.narrow(helloObj);
      if (hello == null) {
        logger.fine("Faceta encontrada não implementa Hello.");
        continue;
      }
      String expected = "Hello " + entity + "!";
      String sayHello = hello.sayHello();
      assert expected.equals(sayHello) : sayHello;
    }
    connection.logout();
  }
}
