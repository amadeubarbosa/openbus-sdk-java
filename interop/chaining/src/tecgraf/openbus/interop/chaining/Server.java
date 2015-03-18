package tecgraf.openbus.interop.chaining;

import java.security.interfaces.RSAPrivateKey;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Configs;
import tecgraf.openbus.interop.util.PrivateKeyInvalidLoginCallback;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;
import tecgraf.openbus.security.Cryptography;

/**
 * Parte servidora do demo Hello
 * 
 * @author Tecgraf
 */
public final class Server {

  /**
   * Função principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String host = configs.bushost;
    int port = configs.busport;
    String entity = "interop_chaining_java_server";
    String privateKeyFile = "admin/InteropChaining.key";
    RSAPrivateKey privateKey =
      Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    Utils.setTestLogLevel(configs.testlog);
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB(args);
    ShutdownThread shutdown = new ShutdownThread(orb);
    Runtime.getRuntime().addShutdownHook(shutdown);

    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection conn = context.createConnection(host, port);
    context.setDefaultConnection(conn);
    conn.loginByCertificate(entity, privateKey);
    // TODO republicar oferta em invalidLogin
    PrivateKeyInvalidLoginCallback callback =
      new PrivateKeyInvalidLoginCallback(entity, privateKey);
    conn.onInvalidLoginCallback(callback);

    shutdown.addConnetion(conn);

    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    ComponentId id =
      new ComponentId("RestrictedHello", (byte) 1, (byte) 0, (byte) 0, "java");
    ComponentContext component = new ComponentContext(orb, poa, id);
    component.addFacet("Hello", HelloHelper.id(), new HelloImpl(context));

    ServiceProperty[] serviceProperties =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Interoperability Tests") };
    OfferRegistry registry = context.getOfferRegistry();
    ServiceOffer offer =
      registry.registerService(component.getIComponent(), serviceProperties);
    callback.addOffer(component.getIComponent(), serviceProperties);
    shutdown.addOffer(offer);
    new ORBRunThread(orb).start();
  }
}
