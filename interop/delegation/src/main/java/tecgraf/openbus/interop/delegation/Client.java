package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.Utils;

public class Client {

  private static final String demo = "demo";
  private static final String forwarder =
    "interop_delegation_(cpp|java|lua|csharp)_forwarder";
  private static final String broadcaster =
    "interop_delegation_(cpp|java|lua|csharp)_broadcaster";
  private static final String expectedfrom = "steve->" + broadcaster;
  private static final String expectedmsg = "Testing the list!";

  public static enum User {
    willian(forwarder, "forwarded message by steve->" + broadcaster + ": "
      + expectedmsg),
    bill("", ""),
    paul(expectedfrom, expectedmsg),
    mary(expectedfrom, expectedmsg),
    steve(expectedfrom, expectedmsg);

    public Pattern from;
    public Pattern msg;

    User(String from, String msg) {
      this.from = Pattern.compile(from);
      this.msg = Pattern.compile(msg);
    }
  }

  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.busref;
    String entity = "interop_delegation_java_client";
    String domain = configs.domain;
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();
    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, entity.getBytes(), domain);
    context.setDefaultConnection(conn);

    ServiceProperty[] serviceProperties = new ServiceProperty[1];
    serviceProperties[0] =
      new ServiceProperty("offer.domain", "Interoperability Tests");
    List<ServiceOfferDesc> offers =
      LibUtils.findOffer(context.getOfferRegistry(), serviceProperties, 3, 10,
        1);

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
    assert forwarder != null;
    assert messenger != null;
    assert broadcaster != null;

    conn.loginByPassword(User.bill.name(), User.bill.name().getBytes(), domain);
    forwarder.setForward(User.willian.name());
    broadcaster.subscribe();
    conn.logout();

    conn.loginByPassword(User.paul.name(), User.paul.name().getBytes(), domain);
    broadcaster.subscribe();
    conn.logout();

    conn.loginByPassword(User.mary.name(), User.mary.name().getBytes(), domain);
    broadcaster.subscribe();
    conn.logout();

    conn.loginByPassword(User.steve.name(), User.steve.name().getBytes(),
      domain);
    broadcaster.subscribe();
    broadcaster.post("Testing the list!");
    conn.logout();

    // Esperando que as mensagens se propaguem
    Thread.sleep(10000);

    List<User> users = new ArrayList<User>();
    users.add(User.willian);
    users.add(User.bill);
    users.add(User.paul);
    users.add(User.mary);
    users.add(User.steve);

    for (User user : users) {
      conn.loginByPassword(user.name(), user.name().getBytes(), domain);
      PostDesc[] posts = messenger.receivePosts();
      switch (user) {
        case bill:
          assert posts.length == 0 : posts.length;
          break;

        default:
          assert posts.length == 1 : user.name() + ":" + posts.length;
          PostDesc desc = posts[0];
          assert user.from.matcher(desc.from).matches() : desc.from;
          assert user.msg.matcher(desc.message).matches() : desc.message;
          break;
      }
      broadcaster.unsubscribe();
      conn.logout();
    }

    conn.loginByPassword(User.bill.name(), User.bill.name().getBytes(), domain);
    forwarder.cancelForward(User.willian.name());
    conn.logout();
  }
}