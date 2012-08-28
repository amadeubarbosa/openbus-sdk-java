package tecgraf.openbus.interop.multiplexing.byorb;

import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;

public class Server {

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = "interop_multiplexing_java_server";
      String privateKeyFile = "admin/InteropMultiplexing.key";
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      OpenBusPrivateKey privateKey =
        OpenBusPrivateKey.createPrivateKeyFromFile(privateKeyFile);

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
      OpenBusContext context1 =
        (OpenBusContext) orb1.resolve_initial_references("OpenBusContext");
      Connection conn1 = context1.createConnection(host, port);
      context1.setDefaultConnection(conn1);

      OpenBusContext context2 =
        (OpenBusContext) orb2.resolve_initial_references("OpenBusContext");
      Connection conn2 = context2.createConnection(host, port);
      context2.setDefaultConnection(conn2);

      // setup action on login termination
      conn1.onInvalidLoginCallback(new Callback(conn1, "conn1"));
      conn2.onInvalidLoginCallback(new Callback(conn2, "conn2"));

      // create service SCS component
      POA poa1 = POAHelper.narrow(orb1.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext component1 = new ComponentContext(orb1, poa1, id);
      component1
        .addFacet("Hello", HelloHelper.id(), new HelloServant(context1));

      POA poa2 = POAHelper.narrow(orb2.resolve_initial_references("RootPOA"));
      poa2.the_POAManager().activate();
      ComponentContext component2 = new ComponentContext(orb2, poa2, id);
      component2
        .addFacet("Hello", HelloHelper.id(), new HelloServant(context2));

      // login to the bus
      conn1.loginByCertificate(entity, privateKey);
      conn2.loginByCertificate(entity, privateKey);

      shutdown1.addConnetion(conn1);
      shutdown2.addConnetion(conn2);

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      context1.getOfferRegistry().registerService(component1.getIComponent(),
        serviceProperties);
      context2.getOfferRegistry().registerService(component1.getIComponent(),
        serviceProperties);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static class Callback implements InvalidLoginCallback {

    private String name;
    private Connection conn;

    public Callback(Connection conn, String name) {
      this.name = name;
      this.conn = conn;
    }

    @Override
    public void invalidLogin(Connection conn, LoginInfo login) {
      System.out.println("login terminated: " + name);
    }
  }

}
