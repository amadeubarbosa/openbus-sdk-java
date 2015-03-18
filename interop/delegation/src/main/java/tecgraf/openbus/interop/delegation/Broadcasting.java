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
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.PrivateKeyInvalidLoginCallback;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.LibUtils.ORBRunThread;
import tecgraf.openbus.utils.LibUtils.ShutdownThread;
import tecgraf.openbus.utils.Utils;

public class Broadcasting {

  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.bus2ref;
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
    context.setDefaultConnection(conn);
    conn.loginByCertificate(entity, privateKey);
    PrivateKeyInvalidLoginCallback callback =
      new PrivateKeyInvalidLoginCallback(entity, privateKey);
    conn.onInvalidLoginCallback(callback);
    shutdown.addConnetion(conn);

    ServiceProperty[] messengerProps = new ServiceProperty[2];
    messengerProps[0] =
      new ServiceProperty("openbus.component.interface", MessengerHelper.id());
    messengerProps[1] =
      new ServiceProperty("offer.domain", "Interoperability Tests");
    List<ServiceOfferDesc> services =
      LibUtils.findOffer(context.getOfferRegistry(), messengerProps, 1, 10, 1);

    Messenger messenger =
      MessengerHelper.narrow(services.get(0).service_ref
        .getFacet(MessengerHelper.id()));

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
    ServiceOffer offer =
      context.getOfferRegistry().registerService(ic, serviceProperties);
    callback.addOffer(ic, serviceProperties);
    shutdown.addOffer(offer);
  }
}
