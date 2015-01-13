package tecgraf.openbus.interop.reloggedjoin;

import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.logging.Level;

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
import tecgraf.openbus.interop.simple.HelloHelper;
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
    Properties props = Utils.readPropertyFile("/test.properties");
    String iorfile = props.getProperty("bus.ior");
    String entity = "interop_reloggedjoin_java_server";
    String privateKeyFile = "admin/InteropReloggedJoin.key";
    RSAPrivateKey privateKey =
      Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    Utils.setLibLogLevel(Level.parse(props.getProperty("log.lib", "OFF")));

    ORB orb = ORBInitializer.initORB(args);
    new ORBRunThread(orb).start();
    ShutdownThread shutdown = new ShutdownThread(orb);
    Runtime.getRuntime().addShutdownHook(shutdown);

    Object busref = orb.string_to_object(Utils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection conn = context.connectByReference(busref);
    context.setDefaultConnection(conn);
    conn.loginByCertificate(entity, privateKey);
    PrivateKeyInvalidLoginCallback callback =
      new PrivateKeyInvalidLoginCallback(entity, privateKey);
    conn.onInvalidLoginCallback(callback);
    shutdown.addConnetion(conn);

    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    ComponentId id =
      new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
    ComponentContext component = new ComponentContext(orb, poa, id);
    component.addFacet("Hello", HelloHelper.id(), new HelloServerServant(
      context));

    ServiceProperty[] serviceProperties =
      new ServiceProperty[] {
          new ServiceProperty("reloggedjoin.role", "server"),
          new ServiceProperty("offer.domain", "Interoperability Tests") };
    OfferRegistry registry = context.getOfferRegistry();
    ServiceOffer offer =
      registry.registerService(component.getIComponent(), serviceProperties);
    callback.addOffer(component.getIComponent(), serviceProperties);
    shutdown.addOffer(offer);
  }
}
