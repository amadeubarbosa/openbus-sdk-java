package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.util.Utils;

public class Client {

  private static final String demo = "demo";
  private static final String willian = "willian";
  private static final String bill = "bill";
  private static final String paul = "paul";
  private static final String mary = "mary";
  private static final String steve = "steve";

  public static void main(String[] args) {
    try {
      Properties properties = Utils.readPropertyFile("/delegation.properties");
      String host = properties.getProperty("host");
      int port = Integer.valueOf(properties.getProperty("port"));

      ORB orb = ORBInitializer.initORB();
      ConnectionManager connections =
        (ConnectionManager) orb
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn = connections.createConnection(host, port);
      conn.loginByPassword("demo", "demo".getBytes());
      connections.setDefaultConnection(conn);

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] offers = conn.offers().findServices(serviceProperties);
      if (offers.length != 3) {
        System.err.println("serviços ofertados errados");
        return;
      }

      Forwarder forwarder = null;
      Messenger messenger = null;
      Broadcaster broadcaster = null;
      for (ServiceOfferDesc desc : offers) {
        for (ServiceProperty prop : desc.properties) {
          if (prop.name.equals("openbus.component.interface")) {
            if (prop.value.equals(ForwarderHelper.id())) {
              Object facet = desc.service_ref.getFacet(ForwarderHelper.id());
              forwarder = ForwarderHelper.narrow(facet);
            }
            else if (prop.value.equals(MessengerHelper.id())) {
              Object facet = desc.service_ref.getFacet(MessengerHelper.id());
              messenger = MessengerHelper.narrow(facet);
            }
            else if (prop.value.equals(BroadcasterHelper.id())) {
              Object facet = desc.service_ref.getFacet(BroadcasterHelper.id());
              broadcaster = BroadcasterHelper.narrow(facet);
            }
          }
        }
      }

      conn.logout();

      conn.loginByPassword(willian, willian.getBytes());
      forwarder.setForward(bill);
      broadcaster.subscribe();
      conn.logout();

      conn.loginByPassword(paul, paul.getBytes());
      broadcaster.subscribe();
      conn.logout();

      conn.loginByPassword(mary, mary.getBytes());
      broadcaster.subscribe();
      conn.logout();

      conn.loginByPassword(steve, steve.getBytes());
      broadcaster.subscribe();
      broadcaster.post("Testing the list!");
      conn.logout();

      System.out.println("Esperando que as mensagens se propaguem...");
      Thread.sleep(10000);
      System.out.println("Completado!");

      List<String> users = new ArrayList<String>();
      users.add(willian);
      users.add(bill);
      users.add(paul);
      users.add(mary);
      users.add(steve);

      for (String user : users) {
        conn.loginByPassword(user, user.getBytes());
        showPostsOf(user, messenger.receivePosts());
        broadcaster.unsubscribe();
        conn.logout();
      }

      conn.loginByPassword(willian, willian.getBytes());
      forwarder.cancelForward(bill);
      conn.logout();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  static void showPostsOf(String user, PostDesc[] posts) {
    System.out.println(String.format("%s recebeu %d mensagens:", user,
      posts.length));
    int i = 1;
    for (PostDesc post : posts) {
      System.out.println(String
        .format("%d) %s: %s", i, post.from, post.message));
      ++i;
    }
    System.out.println();
  }
}
