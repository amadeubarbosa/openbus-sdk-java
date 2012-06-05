package tecgraf.openbus.interop.delegation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import tecgraf.openbus.CallerChain;
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

public class ForwarderServant extends ForwarderPOA {

  public static final String forwarder = "forwarder";

  private Connection conn;
  private Map<String, ForwardInfo> forwardsOf;

  public ForwarderServant(Connection conn) {
    this.conn = conn;
    this.forwardsOf =
      Collections.synchronizedMap(new HashMap<String, ForwardInfo>());
  }

  @Override
  public void setForward(String to) {
    CallerChain chain = conn.getCallerChain();
    LoginInfo[] callers = chain.callers();
    String user = callers[0].entity;
    System.out.println(String.format("configurando forward para '%s' por '%s'",
      to, Utils.chain2str(callers)));
    this.forwardsOf.put(user, new ForwardInfo(chain, to));
  }

  @Override
  public void cancelForward(String to) {
    LoginInfo[] callers = conn.getCallerChain().callers();
    String user = callers[0].entity;
    ForwardInfo forward = this.forwardsOf.remove(user);
    if (forward != null) {
      System.out.println(String.format("cancelando forward para '%s' por '%s'",
        forward.to, Utils.chain2str(callers)));
    }
  }

  @Override
  public String getForward() throws NoForward {
    LoginInfo[] callers = conn.getCallerChain().callers();
    String user = callers[0].entity;
    ForwardInfo forward = this.forwardsOf.get(user);
    if (forward == null) {
      throw new NoForward();
    }
    return forward.to;
  }

  public Map<String, ForwardInfo> getForwardsOf() {
    return forwardsOf;
  }

  public static class ForwardInfo {
    public CallerChain chain;
    public String to;

    public ForwardInfo(CallerChain chain, String to) {
      this.chain = chain;
      this.to = to;
    }
  }

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/delegation.properties");
      String host = props.getProperty("host");
      int port = Integer.valueOf(props.getProperty("port"));

      final ORB orb3 = ORBInitializer.initORB(args);
      new ORBRunThread(orb3).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb3));

      ConnectionManager connections =
        (ConnectionManager) orb3
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn3 = connections.createConnection(host, port);
      connections.setDefaultConnection(conn3);

      ForwarderServant forwarderServant = new ForwarderServant(conn3);
      POA poa3 = POAHelper.narrow(orb3.resolve_initial_references("RootPOA"));
      poa3.the_POAManager().activate();
      ComponentContext context3 =
        new ComponentContext(orb3, poa3, new ComponentId("Forwarder", (byte) 1,
          (byte) 0, (byte) 0, "java"));
      context3.addFacet(ForwarderServant.forwarder, ForwarderHelper.id(),
        forwarderServant);

      conn3.loginByPassword(ForwarderServant.forwarder,
        ForwarderServant.forwarder.getBytes());

      ServiceProperty[] messengerProps = new ServiceProperty[3];
      messengerProps[0] =
        new ServiceProperty("openbus.offer.entity", MessengerServant.messenger);
      messengerProps[1] =
        new ServiceProperty("openbus.component.facet",
          MessengerServant.messenger);
      messengerProps[2] = new ServiceProperty("offer.domain", "Interoperability Tests");
      ServiceOfferDesc[] conn3find =
        conn3.offers().findServices(messengerProps);

      if (conn3find.length <= 0) {
        System.err.println("não encontrou o serviço messenger");
        System.exit(1);
      }

      Messenger conn3messenger =
        MessengerHelper.narrow(conn3find[0].service_ref
          .getFacetByName(MessengerServant.messenger));

      final Timer timer = new Timer(conn3, forwarderServant, conn3messenger);
      timer.start();

      conn3.onInvalidLoginCallback(new InvalidLoginCallback() {

        @Override
        public boolean invalidLogin(Connection conn, LoginInfo login) {
          timer.stopTimer();
          orb3.destroy();
          return false;
        }
      });

      ServiceProperty[] serviceProperties = new ServiceProperty[1];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Interoperability Tests");
      conn3.offers().registerService(context3.getIComponent(),
        serviceProperties);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static class Timer extends Thread {

    private volatile boolean stop = false;
    private ForwarderServant forwarder;
    private Connection conn;
    private Messenger messenger;

    public Timer(Connection conn, ForwarderServant forwarder,
      Messenger messenger) {
      this.forwarder = forwarder;
      this.messenger = messenger;
      this.conn = conn;
    }

    public void stopTimer() {
      this.stop = true;
    }

    @Override
    public void run() {
      while (!stop) {
        try {
          Thread.sleep(5000);
        }
        catch (InterruptedException e) {
          this.stop = true;
        }
        Map<String, ForwardInfo> forwardsOf = forwarder.getForwardsOf();
        synchronized (forwardsOf) {
          for (ForwardInfo info : forwardsOf.values()) {
            System.out.println("Verificando mensagens de " + info.to);
            conn.joinChain(info.chain);
            PostDesc[] posts = messenger.receivePosts();
            conn.exitChain();
            for (PostDesc post : posts) {
              messenger.post(info.to, String.format("forwarded de %s: %s",
                post.from, post.message));
            }
          }
        }
      }
    }
  }
}
