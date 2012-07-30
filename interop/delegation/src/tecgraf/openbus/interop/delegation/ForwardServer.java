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
import tecgraf.openbus.interop.delegation.ForwarderImpl.Timer;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;
import tecgraf.openbus.util.Cryptography;

public class ForwardServer {
  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = "interop_delegation_java_forwarder";
      String privateKeyFile = "admin/InteropDelegation.key";
      byte[] privateKey =
        Cryptography.getInstance().readPrivateKey(privateKeyFile);

      final ORB orb = ORBInitializer.initORB(args);
      new ORBRunThread(orb).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb));

      ConnectionManager connections =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn = connections.createConnection(host, port);
      connections.setDefaultConnection(conn);

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

      ForwarderImpl forwarderServant = new ForwarderImpl(conn);
      POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      poa.the_POAManager().activate();
      ComponentContext ctx =
        new ComponentContext(orb, poa, new ComponentId("Forwarder", (byte) 1,
          (byte) 0, (byte) 0, "java"));
      ctx.addFacet("forwarder", ForwarderHelper.id(), forwarderServant);

      final Timer timer = new Timer(conn, forwarderServant, messenger);
      timer.start();

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      IComponent ic = ctx.getIComponent();
      conn.offers().registerService(ic, serviceProperties);
      conn.onInvalidLoginCallback(new ForwarderInvalidLoginCallback(entity,
        privateKey, ic, serviceProperties, timer));

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
