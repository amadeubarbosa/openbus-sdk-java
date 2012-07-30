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
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;
import tecgraf.openbus.util.Cryptography;

public class MessengerServer {
  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = "interop_delegation_java_messenger";
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

      POA poa1 = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();
      ComponentContext ctx =
        new ComponentContext(orb, poa1, new ComponentId("Messenger", (byte) 1,
          (byte) 0, (byte) 0, "java"));
      ctx.addFacet("messenger", MessengerHelper.id(), new MessengerImpl(conn));

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");

      IComponent ic = ctx.getIComponent();
      conn.offers().registerService(ctx.getIComponent(), serviceProperties);
      conn.onInvalidLoginCallback(new CommonInvalidLoginCallback(entity,
        privateKey, ic, serviceProperties));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
