package tecgraf.openbus.demo.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.StandardOpenBus;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.demo.util.Utils.ORBDestroyThread;
import tecgraf.openbus.demo.util.Utils.ORBRunThread;

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

      OpenBus openbus = StandardOpenBus.getInstance();
      final BusORB orb1 = openbus.initORB(args);
      new ORBRunThread(orb1.getORB()).start();
      Runtime.getRuntime().addShutdownHook(new ORBDestroyThread(orb1.getORB()));
      orb1.activateRootPOAManager();

      Connection conn1 = openbus.connect(host, port, orb1);
      conn1.onInvalidLoginCallback(new InvalidLoginCallback() {

        @Override
        public boolean invalidLogin(Connection conn, LoginInfo login) {
          System.out.println(String.format(
            "login terminated, shutting the server down: %s", login.entity));
          try {
            conn.close();
          }
          catch (ServiceFailure e) {
            e.printStackTrace();
          }
          orb1.getORB().destroy();
          return false;
        }
      });

      ComponentContext context1 =
        new ComponentContext(orb1.getORB(), orb1.getRootPOA(), new ComponentId(
          "Messenger", (byte) 1, (byte) 0, (byte) 0, "java"));
      context1.addFacet(MessengerServant.messenger, MessengerHelper.id(),
        new MessengerServant(conn1));

      conn1.loginByPassword(MessengerServant.messenger,
        MessengerServant.messenger.getBytes());

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "OpenBus Demos");

      conn1.offers().registerService(context1.getIComponent(),
        serviceProperties);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
