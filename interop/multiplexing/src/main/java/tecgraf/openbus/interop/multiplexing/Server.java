package tecgraf.openbus.interop.multiplexing;

import java.security.interfaces.RSAPrivateKey;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.PortableServer.POA;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.LibUtils.ORBRunThread;
import tecgraf.openbus.utils.LibUtils.ShutdownThread;
import tecgraf.openbus.utils.Utils;

public class Server {

  private static final Logger logger = Logger.getLogger(Server.class.getName());

  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String ior1 = configs.busref;
    String ior2 = configs.bus2ref;
    String entity = "interop_multiplexing_java_server";
    String privateKeyFile = "admin/InteropMultiplexing.key";
    Utils.setTestLogLevel(configs.testlog);
    Utils.setLibLogLevel(configs.log);

    RSAPrivateKey privateKey =
      Cryptography.getInstance().readKeyFromFile(privateKeyFile);

    // setup and start the orb
    ORB orb1 = ORBInitializer.initORB(args);
    new ORBRunThread(orb1).start();
    ShutdownThread shutdown1 = new ShutdownThread(orb1);
    Runtime.getRuntime().addShutdownHook(shutdown1);

    ORB orb2 = ORBInitializer.initORB(args);
    new ORBRunThread(orb2).start();
    ShutdownThread shutdown2 = new ShutdownThread(orb2);
    Runtime.getRuntime().addShutdownHook(shutdown2);

    // connect to the bus
    Object bus1orb1 = orb1.string_to_object(LibUtils.file2IOR(ior1));
    Object bus2orb1 = orb1.string_to_object(LibUtils.file2IOR(ior2));
    Object bus1orb2 = orb2.string_to_object(LibUtils.file2IOR(ior1));

    OpenBusContext context1 =
      (OpenBusContext) orb1.resolve_initial_references("OpenBusContext");
    OpenBusContext context2 =
      (OpenBusContext) orb2.resolve_initial_references("OpenBusContext");

    final Connection conn1AtBus1WithOrb1 =
      context1.connectByReference(bus1orb1);
    final Connection conn2AtBus1WithOrb1 = context1.connectByReference
      (bus1orb1);
    final Connection conn1AtBus2WithOrb1 =
      context1.connectByReference(bus2orb1);
    final Connection conn3AtBus1WithOrb2 =
      context2.connectByReference(bus1orb2);

    // create service SCS component
    ComponentId id =
      new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
    POA poa1 = context1.POA();
    ComponentContext component1 = new ComponentContext(orb1, poa1, id);
    component1.addFacet("Hello", HelloHelper.id(), new HelloServant(context1));
    POA poa2 = context2.POA();
    ComponentContext component2 = new ComponentContext(orb2, poa2, id);
    component2.addFacet("Hello", HelloHelper.id(), new HelloServant(context2));

    // login to the bus
    conn1AtBus1WithOrb1.loginByPrivateKey(entity, privateKey);
    conn2AtBus1WithOrb1.loginByPrivateKey(entity, privateKey);
    conn3AtBus1WithOrb2.loginByPrivateKey(entity, privateKey);
    conn1AtBus2WithOrb1.loginByPrivateKey(entity, privateKey);

    shutdown1.addConnetion(conn1AtBus1WithOrb1);
    shutdown1.addConnetion(conn2AtBus1WithOrb1);
    shutdown1.addConnetion(conn1AtBus2WithOrb1);
    shutdown2.addConnetion(conn3AtBus1WithOrb2);

    final String busId1 = conn1AtBus1WithOrb1.busId();
    final String busId2 = conn1AtBus2WithOrb1.busId();
    context1.onCallDispatch((context, busid, loginId, object_id, operation) -> {
      if (busId1.equals(busid)) {
        return conn1AtBus1WithOrb1;
      }
      else if (busId2.equals(busid)) {
        return conn1AtBus2WithOrb1;
      }
      logger.fine("Não encontrou dispatch!!!");
      return null;
    });

    ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap.
      create();
    serviceProperties.put("offer.domain", "Interoperability Tests");

    conn1AtBus1WithOrb1.offerRegistry().registerService(component1
      .getIComponent(), serviceProperties);

    conn2AtBus1WithOrb1.offerRegistry().registerService(component1
      .getIComponent(), serviceProperties);

    conn1AtBus2WithOrb1.offerRegistry().registerService(component1
      .getIComponent(), serviceProperties);

    context2.onCallDispatch((context, busid, loginId, object_id, operation) -> {
      if (busId1.equals(busid)) {
        return conn3AtBus1WithOrb2;
      }
      logger.fine("Não encontrou dispatch!!!");
      return null;
    });
    conn3AtBus1WithOrb2.offerRegistry().registerService(component2
      .getIComponent(), serviceProperties);
  }
}
