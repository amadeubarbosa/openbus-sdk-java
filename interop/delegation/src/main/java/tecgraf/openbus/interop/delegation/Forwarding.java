package tecgraf.openbus.interop.delegation;

import java.security.interfaces.RSAPrivateKey;
import java.util.List;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
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
    context.setDefaultConnection(conn);

    conn.loginByCertificate(entity, privateKey);
    shutdown.addConnetion(conn);
    OfferRegistry offers = context.getOfferRegistry();

    ServiceProperty[] messengerProps = new ServiceProperty[2];
    messengerProps[0] =
      new ServiceProperty("openbus.component.interface", MessengerHelper.id());
    messengerProps[1] =
      new ServiceProperty("offer.domain", "Interoperability Tests");
    List<ServiceOfferDesc> services =
      LibUtils.findOffer(offers, messengerProps, 1, 10, 1);

    Messenger messenger =
      MessengerHelper.narrow(services.get(0).service_ref
        .getFacet(MessengerHelper.id()));

    ForwarderImpl forwarderServant = new ForwarderImpl(context);
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    ComponentContext ctx =
      new ComponentContext(orb, poa, new ComponentId("Forwarder", (byte) 1,
        (byte) 0, (byte) 0, "java"));
    ctx.addFacet("forwarder", ForwarderHelper.id(), forwarderServant);

    final Timer timer = new Timer(context, forwarderServant, messenger);
    timer.start();

    ServiceProperty[] serviceProperties = new ServiceProperty[1];
    serviceProperties[0] =
      new ServiceProperty("offer.domain", "Interoperability Tests");
    IComponent ic = ctx.getIComponent();
    ServiceOffer offer = offers.registerService(ic, serviceProperties);
    conn.onInvalidLoginCallback(new ForwarderInvalidLoginCallback(entity,
      privateKey, ic, serviceProperties, timer));
    shutdown.addOffer(offer);
  }
}
