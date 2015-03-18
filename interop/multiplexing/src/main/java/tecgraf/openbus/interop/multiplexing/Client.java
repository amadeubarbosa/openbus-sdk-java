package tecgraf.openbus.interop.multiplexing;

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
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.Utils;

/**
 * Cliente do demo de Multiplexação.
 * 
 * @author Tecgraf
 */
public class Client {
  private static final Logger logger = Logger.getLogger(Client.class.getName());

  /**
   * Função principal.
   * 
   * @param args
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String ior1 = configs.busref;
    String ior2 = configs.bus2ref;
    String domain = configs.domain;
    Utils.setTestLogLevel(configs.testlog);
    Utils.setLibLogLevel(configs.log);
    String iors[] = { ior1, ior2 };

    for (String ior : iors) {
      ORB orb = ORBInitializer.initORB();
      Object bus = orb.string_to_object(LibUtils.file2IOR(ior));
      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection conn = context.connectByReference(bus);
      context.setDefaultConnection(conn);
      String login = "interop_multiplexing_java_client";
      conn.loginByPassword(login, login.getBytes(), domain);

      int noffers;
      if (ior.equals(ior1)) {
        noffers = 3;
      }
      else {
        noffers = 1;
      }
      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      serviceProperties[0] =
        new ServiceProperty("openbus.component.interface", HelloHelper.id());
      serviceProperties[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      List<ServiceOfferDesc> services =
        LibUtils.findOffer(context.getOfferRegistry(), serviceProperties,
          noffers, 10, 1);
      for (ServiceOfferDesc offer : services) {
        logger.fine(String
          .format("found offer from %s on bus %s", LibUtils.findProperty(
            offer.properties, "openbus.offer.entity"), conn.busid()));
        org.omg.CORBA.Object obj = offer.service_ref.getFacet(HelloHelper.id());
        Hello hello = HelloHelper.narrow(obj);
        String expected = String.format("Hello %s@%s!", login, conn.busid());
        String sayHello = hello.sayHello();
        assert expected.equals(sayHello) : sayHello;
      }
      conn.logout();
    }
  }
}
