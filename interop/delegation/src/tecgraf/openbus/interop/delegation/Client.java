package tecgraf.openbus.interop.delegation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.omg.CORBA.Object;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.Property;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.exception.RSUnavailableException;

public class Client {

  private static final String willian = "willian";
  private static final String bill = "bill";
  private static final String paul = "paul";
  private static final String mary = "mary";
  private static final String steve = "steve";

  public static void main(String[] args) throws IOException {
    Properties props = new Properties();
    InputStream in = Client.class.getResourceAsStream("/test.properties");
    try {
      props.load(in);
    }
    finally {
      in.close();
    }
    try {
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));

      Properties orbProps = new Properties();
      orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
      orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
        "org.jacorb.orb.ORBSingleton");
      Openbus bus = Openbus.getInstance();
      bus.initWithFaultTolerance(args, orbProps, host, port);

      String userLogin = props.getProperty("login");
      String userPassword = props.getProperty("password");

      IRegistryService registryService = bus.connect(userLogin, userPassword);
      if (registryService == null) {
        throw new RSUnavailableException();
      }

      Property[] serviceProperties = new Property[1];
      serviceProperties[0] =
        new Property("offer.domain", new String[] { "Interoperability Tests" });
      ServiceOffer[] offers =
        registryService.findByCriteria(new String[] {}, serviceProperties);

      if (offers.length != 3) {
        System.err.println("serviços ofertados errados");
        return;
      }

      Forwarder forwarder = null;
      Messenger messenger = null;
      Broadcaster broadcaster = null;
      for (ServiceOffer offer : offers) {
        for (Property prop : offer.properties) {
          if (prop.name.equals("openbus.component.interface")) {
            if (prop.value[0].equals(ForwarderHelper.id())) {
              Object facet = offer.member.getFacet(ForwarderHelper.id());
              forwarder = ForwarderHelper.narrow(facet);
            }
            else if (prop.value[0].equals(MessengerHelper.id())) {
              Object facet = offer.member.getFacet(MessengerHelper.id());
              messenger = MessengerHelper.narrow(facet);
            }
            else if (prop.value[0].equals(BroadcasterHelper.id())) {
              Object facet = offer.member.getFacet(BroadcasterHelper.id());
              broadcaster = BroadcasterHelper.narrow(facet);
            }
          }
        }
      }

      bus.disconnect();

      bus.connect(bill, bill);
      forwarder.setForward(willian);
      broadcaster.subscribe();
      bus.disconnect();

      bus.connect(paul, paul);
      broadcaster.subscribe();
      bus.disconnect();

      bus.connect(mary, mary);
      broadcaster.subscribe();
      bus.disconnect();

      bus.connect(steve, steve);
      broadcaster.subscribe();
      broadcaster.post("Testing the list!");
      bus.disconnect();

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
        bus.connect(user, user);
        showPostsOf(user, messenger.receivePosts());
        broadcaster.unsubscribe();
        bus.disconnect();
      }

      bus.connect(bill, bill);
      forwarder.cancelForward(willian);
      bus.disconnect();
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
