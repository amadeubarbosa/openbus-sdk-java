package tecgraf.openbus.interop.chaining;

import java.security.interfaces.RSAPrivateKey;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.PortableServer.POA;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.*;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.LibUtils.ORBRunThread;
import tecgraf.openbus.utils.LibUtils.ShutdownThread;
import tecgraf.openbus.utils.Utils;

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
    String iorfile = configs.busref;
    String entity = "interop_chaining_java_server";
    String privateKeyFile = "admin/InteropChaining.key";
    RSAPrivateKey privateKey =
      Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    Utils.setTestLogLevel(configs.testlog);
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB(args);
    ShutdownThread shutdown = new ShutdownThread(orb);
    Runtime.getRuntime().addShutdownHook(shutdown);

    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection conn = context.connectByReference(busref);
    context.defaultConnection(conn);
    conn.loginByPrivateKey(entity, privateKey);

    shutdown.addConnetion(conn);

    POA poa = context.POA();
    ComponentId id =
      new ComponentId("RestrictedHello", (byte) 1, (byte) 0, (byte) 0, "java");
    ComponentContext component = new ComponentContext(orb, poa, id);
    component.addFacet("Hello", HelloHelper.id(), new HelloImpl(context));

    ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
      .create();
    serviceProperties.put("offer.domain", "Interoperability Tests");
    OfferRegistry registry = conn.offerRegistry();
    LocalOffer localOffer =
      registry.registerService(component.getIComponent(), serviceProperties);
    RemoteOffer myOffer = localOffer.remoteOffer(60000);
    if (myOffer != null) {
      shutdown.addOffer(myOffer);
    } else {
      localOffer.remove();
      shutdown.run();
      System.exit(1);
    }
    new ORBRunThread(orb).start();
  }
}
