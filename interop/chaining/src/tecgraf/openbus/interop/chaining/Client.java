package tecgraf.openbus.interop.chaining;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.interop.util.Utils;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class Client {
  /**
   * Fun��o principal.
   * 
   * @param args argumentos.
   * @throws AlreadyLoggedIn
   * @throws InvalidName
   * @throws ServiceFailure
   * @throws AccessDenied
   * @throws IOException
   */
  public static void main(String[] args) throws Exception {
    Properties props = Utils.readPropertyFile("/test.properties");
    String host = props.getProperty("bus.host.name");
    int port = Integer.valueOf(props.getProperty("bus.host.port"));
    String entity = "interop_chaining_java_client";
    Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

    ORB orb = ORBInitializer.initORB(args);
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.createConnection(host, port);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes());
    ServiceProperty[] properties =
      new ServiceProperty[] {
          new ServiceProperty("offer.domain", "Interoperability Tests"),
          new ServiceProperty("openbus.component.interface", HelloProxyHelper
            .id()) };
    List<ServiceOfferDesc> services =
      Utils.findOffer(context.getOfferRegistry(), properties, 1, 10, 1);

    for (ServiceOfferDesc desc : services) {
      org.omg.CORBA.Object msgObj =
        desc.service_ref.getFacet(HelloProxyHelper.id());
      if (msgObj == null) {
        System.out.println("o servi�o encontrado n�o prov� a faceta ofertada");
        continue;
      }
      String loginId =
        Utils.findProperty(desc.properties, "openbus.offer.login");
      CallerChain chain = context.makeChainFor(loginId);
      byte[] encodedChain = context.encodeChain(chain);
      HelloProxy proxy = HelloProxyHelper.narrow(msgObj);
      String sayHello = proxy.fetchHello(encodedChain);

      String expected = "Hello " + entity + "!";
      if (expected.equals(sayHello)) {
        System.out.println("Received: " + sayHello);
      }
      else {
        System.err.println("ERROR!");
        System.err.println("Expected: " + expected);
        System.err.println("Received: " + sayHello);
      }
    }

    context.getCurrentConnection().logout();
  }

}