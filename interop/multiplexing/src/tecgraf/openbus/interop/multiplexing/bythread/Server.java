package tecgraf.openbus.interop.multiplexing.bythread;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;

public class Server {

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      String host2 = props.getProperty("bus2.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      int port2 = Integer.valueOf(props.getProperty("bus2.host.port"));
      String entity = "interop_multiplexing_java_server";
      String privateKeyFile = "admin/InteropMultiplexing.key";
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      OpenBusPrivateKey privateKey =
        OpenBusPrivateKey.createPrivateKeyFromFile(privateKeyFile);

      // setup and start the orb
      ORB orb = ORBInitializer.initORB(args);
      new ORBRunThread(orb).start();
      ShutdownThread shutdown = new ShutdownThread(orb);
      Runtime.getRuntime().addShutdownHook(shutdown);

      // connect to the bus
      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

      final Connection conn1AtBus1 = context.createConnection(host, port);
      Connection conn2AtBus1 = context.createConnection(host, port);
      final Connection connAtBus2 = context.createConnection(host2, port2);

      List<Connection> conns = new ArrayList<Connection>();
      conns.add(conn1AtBus1);
      conns.add(conn2AtBus1);
      conns.add(connAtBus2);

      POA poa1 = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();

      // create service SCS component
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext component1 = new ComponentContext(orb, poa1, id);
      component1.addFacet("hello", HelloHelper.id(), new HelloServant(context));

      // login to the bus
      conn1AtBus1.loginByCertificate(entity, privateKey);
      conn2AtBus1.loginByCertificate(entity, privateKey);
      connAtBus2.loginByCertificate(entity, privateKey);

      shutdown.addConnetion(conn1AtBus1);
      shutdown.addConnetion(conn2AtBus1);
      shutdown.addConnetion(connAtBus2);

      final String busId1 = conn1AtBus1.busid();
      final String busId2 = connAtBus2.busid();
      // Set incoming connections
      context.onCallDispatch(new CallDispatchCallback() {

        @Override
        public Connection dispatch(OpenBusContext context, String busid,
          String loginId, byte[] object_id, String operation) {
          if (busId1.equals(busid)) {
            return conn1AtBus1;
          }
          else if (busId2.equals(busid)) {
            return connAtBus2;
          }
          System.err.println("Não encontrou dispatch!!!");
          return null;
        }
      });

      Thread thread1 =
        new RegisterThread(conn1AtBus1, context, component1.getIComponent());
      thread1.start();
      conn1AtBus1.onInvalidLoginCallback(new Callback(conn1AtBus1,
        "conn1AtBus1"));

      Thread thread2 =
        new RegisterThread(conn2AtBus1, context, component1.getIComponent());
      thread2.start();
      conn2AtBus1.onInvalidLoginCallback(new Callback(conn2AtBus1,
        "conn2AtBus1"));

      context.setCurrentConnection(connAtBus2);
      context.getOfferRegistry().registerService(component1.getIComponent(),
        getProps());
      connAtBus2.onInvalidLoginCallback(new Callback(connAtBus2, "connAtBus2"));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ServiceProperty[] getProps() {
    ServiceProperty[] serviceProperties = new ServiceProperty[1];
    serviceProperties[0] =
      new ServiceProperty("offer.domain", "Interoperability Tests");
    return serviceProperties;
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

  private static class RegisterThread extends Thread {

    private Connection conn;
    private OpenBusContext context;
    private IComponent component;

    public RegisterThread(Connection conn, OpenBusContext context,
      IComponent component) {
      this.conn = conn;
      this.context = context;
      this.component = component;
    }

    @Override
    public void run() {
      context.setCurrentConnection(conn);
      try {
        context.getOfferRegistry().registerService(component, getProps());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  };

}
