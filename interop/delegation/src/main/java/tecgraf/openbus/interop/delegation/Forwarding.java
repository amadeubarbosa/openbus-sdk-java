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
import tecgraf.openbus.interop.delegation.ForwarderImpl.Timer;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.LibUtils.ORBRunThread;
import tecgraf.openbus.utils.LibUtils.ShutdownThread;
import tecgraf.openbus.utils.Utils;

public class Forwarding {
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.busref;
    String entity = "interop_delegation_java_forwarder";
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
    OfferRegistry offers = conn.offerRegistry();

    ArrayListMultimap<String, String> messengerProps = ArrayListMultimap
      .create();
    messengerProps.put("offer.domain", "Interoperability Tests");
    messengerProps.put("openbus.component.interface", MessengerHelper.id());
    List<RemoteOffer> services =
      LibUtils.findOffer(offers, messengerProps, 1, 10, 1);

    Messenger messenger =
      MessengerHelper.narrow(services.get(0).service()
        .getFacet(MessengerHelper.id()));

    ForwarderImpl forwarderServant = new ForwarderImpl(context);
    POA poa = context.POA();
    ComponentContext ctx =
      new ComponentContext(orb, poa, new ComponentId("Forwarder", (byte) 1,
        (byte) 0, (byte) 0, "java"));
    ctx.addFacet("forwarder", ForwarderHelper.id(), forwarderServant);

    final Timer timer = new Timer(context, forwarderServant, messenger);
    timer.start();

    ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
      .create();
    serviceProperties.put("offer.domain", "Interoperability Tests");
    IComponent ic = ctx.getIComponent();
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
