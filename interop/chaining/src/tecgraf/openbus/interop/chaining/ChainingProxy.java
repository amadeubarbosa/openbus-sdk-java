package tecgraf.openbus.interop.chaining;

import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.logging.Level;

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
import tecgraf.openbus.interop.util.PrivateKeyInvalidLoginCallback;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;
import tecgraf.openbus.security.Cryptography;

/**
 * Parte proxy do teste de interoperabilidade Chaining
 * 
 * @author Tecgraf
 */
public final class ChainingProxy {

  /**
   * Função principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = "interop_chaining_java_proxy";
      String privateKeyFile = "admin/InteropChaining.key";
      RSAPrivateKey privateKey =
        Cryptography.getInstance().readKeyFromFile(privateKeyFile);
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      ORB orb = ORBInitializer.initORB(args);
      new ORBRunThread(orb).start();
      ShutdownThread shutdown = new ShutdownThread(orb);
      Runtime.getRuntime().addShutdownHook(shutdown);

      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection conn = context.createConnection(host, port);
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
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
