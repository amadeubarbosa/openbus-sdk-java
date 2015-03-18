package tecgraf.openbus.interop.chaining;

import java.security.interfaces.RSAPrivateKey;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.PrivateKeyInvalidLoginCallback;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.LibUtils.ORBRunThread;
import tecgraf.openbus.utils.LibUtils.ShutdownThread;
import tecgraf.openbus.utils.Utils;

/**
 * Parte proxy do teste de interoperabilidade Chaining
 * 
 * @author Tecgraf
 */
public final class Proxy {

  /**
   * Função principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.bus2ref;
    String entity = "interop_chaining_java_proxy";
    String privateKeyFile = "admin/InteropChaining.key";
    RSAPrivateKey privateKey =
      Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    Utils.setTestLogLevel(configs.testlog);
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB(args);
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
    conn.onInvalidLoginCallback();

    shutdown.addConnetion(conn);

    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    ComponentId id =
      new ComponentId("HelloProxy", (byte) 1, (byte) 0, (byte) 0, "java");
    ComponentContext component = new ComponentContext(orb, poa, id);
    component.addFacet("HelloProxy", HelloProxyHelper.id(), new ProxyImpl(
      context));

    ServiceProperty[] serviceProperties =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Interoperability Tests") };
    OfferRegistry registry = context.getOfferRegistry();
    ServiceOffer offer =
      registry.registerService(component.getIComponent(), serviceProperties);
    callback.addOffer(component.getIComponent(), serviceProperties);
    shutdown.addOffer(offer);
  }
}
