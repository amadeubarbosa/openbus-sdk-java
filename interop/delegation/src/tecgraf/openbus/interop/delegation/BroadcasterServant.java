package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;

public class BroadcasterServant extends BroadcasterPOA {

  public static final String broadcaster =
    "interop_delegation_java_broadcaster";

  private Connection conn;
  private List<String> subscribers;
  private Messenger messenger;

  public BroadcasterServant(Connection conn, Messenger messenger) {
    this.conn = conn;
    this.subscribers = Collections.synchronizedList(new ArrayList<String>());
    this.messenger = messenger;
  }

  @Override
  public void post(String message) {
    conn.joinChain();
    synchronized (subscribers) {
      for (String user : subscribers) {
        messenger.post(user, message);
      }
    }
  }

  @Override
  public void subscribe() {
    LoginInfo[] callers = conn.getCallerChain().callers();
    String user = callers[0].entity;
    System.out.println("inscrição de " + Utils.chain2str(callers));
    subscribers.add(user);
  }

  @Override
  public void unsubscribe() {
    LoginInfo[] callers = conn.getCallerChain().callers();
    String user = callers[0].entity;
    System.out.println("cancelando inscrição de " + Utils.chain2str(callers));
    subscribers.remove(user);
  }

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/delegation.properties");
      String host = props.getProperty("host");
      int port = Integer.valueOf(props.getProperty("port"));

      final ORB orb2 = ORBInitializer.initORB(args);
      new ORBRunThread(orb2).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb2));

      ConnectionManager connections =
        (ConnectionManager) orb2
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn2 = connections.createConnection(host, port);
      connections.setDefaultConnection(conn2);
      conn2.onInvalidLoginCallback(new InvalidLoginCallback() {

        @Override
        public boolean invalidLogin(Connection conn, LoginInfo login) {
          System.out.println(String.format(
            "login terminated, shutting the server down: %s", login.entity));
          orb2.destroy();
          return false;
        }
      });
      conn2.loginByPassword(BroadcasterServant.broadcaster,
        BroadcasterServant.broadcaster.getBytes());

      ServiceProperty[] messengerProps = new ServiceProperty[3];
      messengerProps[0] =
        new ServiceProperty("openbus.offer.entity", MessengerServant.messenger);
      messengerProps[1] =
        new ServiceProperty("openbus.component.facet",
          MessengerServant.messenger);
      messengerProps[2] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] conn2find =
        conn2.offers().findServices(messengerProps);

      if (conn2find.length <= 0) {
        System.err.println("não encontrou o serviço messenger");
        System.exit(1);
      }

      Messenger conn2messenger =
        MessengerHelper.narrow(conn2find[0].service_ref
          .getFacetByName(MessengerServant.messenger));
      POA poa2 = POAHelper.narrow(orb2.resolve_initial_references("RootPOA"));
      poa2.the_POAManager().activate();
      ComponentContext context2 =
        new ComponentContext(orb2, poa2, new ComponentId("Broadcaster",
          (byte) 1, (byte) 0, (byte) 0, "java"));
      context2.addFacet(BroadcasterServant.broadcaster, BroadcasterHelper.id(),
        new BroadcasterServant(conn2, conn2messenger));

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      conn2.offers().registerService(context2.getIComponent(),
        serviceProperties);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
