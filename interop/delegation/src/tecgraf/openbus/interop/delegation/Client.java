package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
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
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = "interop_delegation_java_client";
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      ORB orb = ORBInitializer.initORB();
      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection conn = context.createConnection(host, port);
      conn.loginByPassword(entity, entity.getBytes());
      context.setDefaultConnection(conn);

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] offers =
        context.getOfferRegistry().findServices(serviceProperties);
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

      conn.loginByPassword(bill, bill.getBytes());
      forwarder.setForward(willian);
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

      conn.loginByPassword(bill, bill.getBytes());
      forwarder.cancelForward(willian);
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
