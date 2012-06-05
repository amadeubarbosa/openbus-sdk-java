package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;

public class MessengerServant extends MessengerPOA {

  public static final String messenger = "messenger";

  private Connection conn;
  private Map<String, List<PostDesc>> inboxOf;

  public MessengerServant(Connection conn) {
    this.conn = conn;
    this.inboxOf =
      Collections.synchronizedMap(new HashMap<String, List<PostDesc>>());
  }

  @Override
  public void post(String to, String message) {
    LoginInfo[] callers = conn.getCallerChain().callers();
    String from = callers[0].entity;
    System.out.println(String.format("post para '%s' de '%s'", to, Utils
      .chain2str(callers)));
    synchronized (inboxOf) {
      List<PostDesc> list = this.inboxOf.get(to);
      if (list == null) {
        list = new ArrayList<PostDesc>();
        inboxOf.put(to, list);
      }
      list.add(new PostDesc(from, message));
    }
  }

  @Override
  public PostDesc[] receivePosts() {
    LoginInfo[] callers = conn.getCallerChain().callers();
    String owner = callers[0].entity;
    System.out
      .println("download das mensagens por " + Utils.chain2str(callers));
    List<PostDesc> list = this.inboxOf.remove(owner);
    if (list == null) {
      list = new ArrayList<PostDesc>();
    }
    return list.toArray(new PostDesc[list.size()]);
  }

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/delegation.properties");
      String host = props.getProperty("host");
      int port = Integer.valueOf(props.getProperty("port"));

      final ORB orb1 = ORBInitializer.initORB(args);
      new ORBRunThread(orb1).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb1));

      ConnectionManager connections =
        (ConnectionManager) orb1
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn1 = connections.createConnection(host, port);
      connections.setDefaultConnection(conn1);
      conn1.onInvalidLoginCallback(new InvalidLoginCallback() {

        @Override
        public boolean invalidLogin(Connection conn, LoginInfo login) {
          System.out.println(String.format(
            "login terminated, shutting the server down: %s", login.entity));
          orb1.destroy();
          return false;
        }
      });

      POA poa1 = POAHelper.narrow(orb1.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();
      ComponentContext context1 =
        new ComponentContext(orb1, poa1, new ComponentId("Messenger", (byte) 1,
          (byte) 0, (byte) 0, "java"));
      context1.addFacet(MessengerServant.messenger, MessengerHelper.id(),
        new MessengerServant(conn1));

      conn1.loginByPassword(MessengerServant.messenger,
        MessengerServant.messenger.getBytes());

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");

      conn1.offers().registerService(context1.getIComponent(),
        serviceProperties);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
