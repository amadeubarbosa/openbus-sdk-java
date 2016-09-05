package tecgraf.openbus.interop.delegation;

import java.security.interfaces.RSAPrivateKey;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.PortableServer.POA;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import tecgraf.openbus.*;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.LibUtils.ORBRunThread;
import tecgraf.openbus.utils.LibUtils.ShutdownThread;
import tecgraf.openbus.utils.Utils;

public class Broadcasting {

  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.busref;
    String entity = "interop_delegation_java_broadcaster";
    String privateKeyFile = "admin/InteropDelegation.key";
    RSAPrivateKey privateKey =
      Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    Utils.setLibLogLevel(configs.log);

    final ORB orb = ORBInitializer.initORB(args);
    new ORBRunThread(orb).start();
    ShutdownThread shutdown = new ShutdownThread(orb);
    Runtime.getRuntime().addShutdownHook(shutdown);

    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection conn = context.connectByReference(busref);
    context.defaultConnection(conn);
    conn.loginByPrivateKey(entity, privateKey);
    shutdown.addConnetion(conn);

    ArrayListMultimap<String, String> messengerProps = ArrayListMultimap
      .create();
    messengerProps.put("offer.domain", "Interoperability Tests");
    messengerProps.put("openbus.component.interface", MessengerHelper.id());
    List<RemoteOffer> services =
      LibUtils.findOffer(conn.offerRegistry(), messengerProps, 1, 10, 1);

    Messenger messenger =
      MessengerHelper.narrow(services.get(0).service()
        .getFacet(MessengerHelper.id()));

    POA poa = context.POA();
    ComponentContext component =
      new ComponentContext(orb, poa, new ComponentId("Broadcaster", (byte) 1,
        (byte) 0, (byte) 0, "java"));
    component.addFacet("broadcaster", BroadcasterHelper.id(),
      new BroadcasterImpl(context, messenger));

    ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
      .create();
    serviceProperties.put("offer.domain", "Interoperability Tests");

    IComponent ic = component.getIComponent();
    OfferRegistry registry = conn.offerRegistry();
    LocalOffer localOffer =
      registry.registerService(ic, serviceProperties);
    RemoteOffer myOffer = localOffer.remoteOffer(60000);
    if (myOffer != null) {
      shutdown.addOffer(myOffer);
    } else {
      localOffer.remove();
      shutdown.run();
      System.exit(1);
    }
  }
}
