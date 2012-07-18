package tecgraf.openbus.interop.delegation;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;
import tecgraf.openbus.util.Cryptography;

public class BroadcasterServer {

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/delegation.properties");
      String entity = props.getProperty("broadcaster_entity");
      String host = props.getProperty("host");
      int port = Integer.valueOf(props.getProperty("port"));
      String privateKeyFile = props.getProperty("key");
      byte[] privateKey =
        Cryptography.getInstance().readPrivateKey(privateKeyFile);

      final ORB orb = ORBInitializer.initORB(args);
      new ORBRunThread(orb).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb));

      ConnectionManager manager =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn = manager.createConnection(host, port);
      manager.setDefaultConnection(conn);

      conn.loginByCertificate(entity, privateKey);

      ServiceProperty[] messengerProps = new ServiceProperty[2];
      messengerProps[0] =
        new ServiceProperty("openbus.component.interface", MessengerHelper.id());
      messengerProps[1] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] services = conn.offers().findServices(messengerProps);

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
      ComponentContext context =
        new ComponentContext(orb, poa, new ComponentId("Broadcaster", (byte) 1,
          (byte) 0, (byte) 0, "java"));
      context.addFacet("broadcaster", BroadcasterHelper.id(),
        new BroadcasterImpl(conn, messenger));

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");

      IComponent ic = context.getIComponent();
      conn.offers().registerService(ic, serviceProperties);
      conn.onInvalidLoginCallback(new CommonInvalidLoginCallback(entity,
        privateKey, ic, serviceProperties));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
