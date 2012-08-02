package tecgraf.openbus.interop.multiplexing.mixed;

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
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.simple.HelloHelper;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;
import tecgraf.openbus.util.Cryptography;

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

      Cryptography crypto = Cryptography.getInstance();
      byte[] privateKey = crypto.readPrivateKey(privateKeyFile);

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
      ConnectionManager manager1 =
        (ConnectionManager) orb1
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      ConnectionManager manager2 =
        (ConnectionManager) orb2
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);

      Connection conn1AtBus1WithOrb1 = manager1.createConnection(host, port);
      Connection conn2AtBus1WithOrb1 = manager1.createConnection(host, port);
      Connection conn1AtBus2WithOrb1 = manager1.createConnection(host2, port2);
      Connection conn3AtBus1WithOrb2 = manager2.createConnection(host, port);

      List<Connection> conns = new ArrayList<Connection>();
      conns.add(conn1AtBus1WithOrb1);
      conns.add(conn1AtBus2WithOrb1);
      conns.add(conn3AtBus1WithOrb2);

      // setup action on login termination
      conn1AtBus1WithOrb1.onInvalidLoginCallback(new Callback(
        conn1AtBus1WithOrb1, "conn1AtBus1WithOrb1"));
      conn2AtBus1WithOrb1.onInvalidLoginCallback(new Callback(
        conn2AtBus1WithOrb1, "conn2AtBus1WithOrb1"));
      conn1AtBus2WithOrb1.onInvalidLoginCallback(new Callback(
        conn1AtBus2WithOrb1, "connAtBus2WithOrb1"));
      conn3AtBus1WithOrb2.onInvalidLoginCallback(new Callback(
        conn3AtBus1WithOrb2, "connAtBus1WithOrb2"));

      // create service SCS component
      ComponentId id =
        new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
      POA poa1 = POAHelper.narrow(orb1.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();
      ComponentContext context1 = new ComponentContext(orb1, poa1, id);
      context1.addFacet("Hello", HelloHelper.id(), new HelloServant(conns));
      POA poa2 = POAHelper.narrow(orb2.resolve_initial_references("RootPOA"));
      poa2.the_POAManager().activate();
      ComponentContext context2 = new ComponentContext(orb2, poa2, id);
      context2.addFacet("Hello", HelloHelper.id(), new HelloServant(conns));

      // login to the bus
      conn1AtBus1WithOrb1.loginByCertificate(entity, privateKey);
      conn2AtBus1WithOrb1.loginByCertificate(entity, privateKey);
      conn3AtBus1WithOrb2.loginByCertificate(entity, privateKey);
      conn1AtBus2WithOrb1.loginByCertificate(entity, privateKey);

      manager1.setDispatcher(conn1AtBus1WithOrb1);
      manager1.setDispatcher(conn1AtBus2WithOrb1);

      shutdown1.addConnetion(conn1AtBus1WithOrb1);
      shutdown1.addConnetion(conn2AtBus1WithOrb1);
      shutdown1.addConnetion(conn1AtBus2WithOrb1);
      shutdown2.addConnetion(conn3AtBus1WithOrb2);

      Thread thread1 =
        new RegisterThread(conn1AtBus1WithOrb1, manager1, context1
          .getIComponent());
      thread1.start();

      Thread thread2 =
        new RegisterThread(conn2AtBus1WithOrb1, manager1, context1
          .getIComponent());
      thread2.start();

      manager1.setRequester(conn1AtBus2WithOrb1);
      conn1AtBus2WithOrb1.offers().registerService(context1.getIComponent(),
        getProps());

      manager2.setRequester(conn3AtBus1WithOrb2);
      manager2.setDispatcher(conn3AtBus1WithOrb2);
      conn3AtBus1WithOrb2.offers().registerService(context2.getIComponent(),
        getProps());
      manager2.setRequester(null);
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
    private ConnectionManager multiplexer;
    private IComponent component;

    public RegisterThread(Connection conn, ConnectionManager multiplexer,
      IComponent component) {
      this.conn = conn;
      this.multiplexer = multiplexer;
      this.component = component;
    }

    @Override
    public void run() {
      multiplexer.setRequester(conn);
      try {
        conn.offers().registerService(component, getProps());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  };

}
