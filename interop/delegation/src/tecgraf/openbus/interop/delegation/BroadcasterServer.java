package tecgraf.openbus.interop.delegation;

import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;

public class BroadcasterServer {

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = "interop_delegation_java_broadcaster";
      String privateKeyFile = "admin/InteropDelegation.key";
      OpenBusPrivateKey privateKey =
        OpenBusPrivateKey.createPrivateKeyFromFile(privateKeyFile);
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      final ORB orb = ORBInitializer.initORB(args);
      new ORBRunThread(orb).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb));

      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection conn = context.createConnection(host, port);
      context.setDefaultConnection(conn);

      conn.loginByCertificate(entity, privateKey);

      ServiceProperty[] messengerProps = new ServiceProperty[2];
      messengerProps[0] =
        new ServiceProperty("openbus.component.interface", MessengerHelper.id());
      messengerProps[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] services =
        context.getOfferRegistry().findServices(messengerProps);

      if (services.length <= 0) {
        System.err.println("não encontrou o serviço messenger");
        System.exit(1);
      }
      if (services.length > 1) {
        System.out.println("Foram encontrados vários serviços de messenger");
      }

      Messenger messenger =
        MessengerHelper.narrow(services[0].service_ref.getFacet(MessengerHelper
          .id()));

      POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      poa.the_POAManager().activate();
      ComponentContext component =
        new ComponentContext(orb, poa, new ComponentId("Broadcaster", (byte) 1,
          (byte) 0, (byte) 0, "java"));
      component.addFacet("broadcaster", BroadcasterHelper.id(),
        new BroadcasterImpl(context, messenger));

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");

      IComponent ic = component.getIComponent();
      context.getOfferRegistry().registerService(ic, serviceProperties);
      conn.onInvalidLoginCallback(new CommonInvalidLoginCallback(entity,
        privateKey, ic, serviceProperties));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
