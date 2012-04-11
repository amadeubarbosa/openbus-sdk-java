package tecgraf.openbus.demo.delegation;

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
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.delegation.ForwarderServant.Timer;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.demo.util.Utils.ORBRunThread;
import tecgraf.openbus.demo.util.Utils.ShutdownThread;

public class Server {

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/delegation.properties");
      String host = props.getProperty("host");
      int port = Integer.valueOf(props.getProperty("port"));

      OpenBus openbus = StandardOpenBus.getInstance();
      final BusORB orb1 = openbus.initORB(args);
      new ORBRunThread(orb1.getORB()).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb1.getORB()));
      orb1.activateRootPOAManager();

      final BusORB orb2 = openbus.initORB(args);
      new ORBRunThread(orb2.getORB()).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb2.getORB()));
      orb2.activateRootPOAManager();

      final BusORB orb3 = openbus.initORB(args);
      new ORBRunThread(orb3.getORB()).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb3.getORB()));
      orb3.activateRootPOAManager();

      /********** Messenger ***********/
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

      /********** Broadcaster ***********/
      Connection conn2 = openbus.connect(host, port, orb2);
      conn2.onInvalidLoginCallback(new InvalidLoginCallback() {

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
          orb2.getORB().destroy();
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
      messengerProps[2] = new ServiceProperty("offer.domain", "OpenBus Demos");
      ServiceOfferDesc[] conn2find =
        conn2.offers().findServices(messengerProps);

      if (conn2find.length <= 0) {
        System.err.println("não encontrou o serviço messenger");
        System.exit(1);
      }

      Messenger conn2messenger =
        MessengerHelper.narrow(conn2find[0].service_ref
          .getFacetByName(MessengerServant.messenger));
      ComponentContext context2 =
        new ComponentContext(orb2.getORB(), orb2.getRootPOA(), new ComponentId(
          "Broadcaster", (byte) 1, (byte) 0, (byte) 0, "java"));
      context2.addFacet(BroadcasterServant.broadcaster, BroadcasterHelper.id(),
        new BroadcasterServant(conn2, conn2messenger));

      conn2.offers().registerService(context2.getIComponent(),
        serviceProperties);

      /********** Forwarder ***********/
      Connection conn3 = openbus.connect(host, port, orb3);
      ForwarderServant forwarderServant = new ForwarderServant(conn3);
      ComponentContext context3 =
        new ComponentContext(orb3.getORB(), orb3.getRootPOA(), new ComponentId(
          "Forwarder", (byte) 1, (byte) 0, (byte) 0, "java"));
      context3.addFacet(ForwarderServant.forwarder, ForwarderHelper.id(),
        forwarderServant);

      conn3.loginByPassword(ForwarderServant.forwarder,
        ForwarderServant.forwarder.getBytes());

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
          try {
            conn.close();
          }
          catch (ServiceFailure e) {
            e.printStackTrace();
          }
          orb3.getORB().destroy();
          return false;
        }
      });

      conn3.offers().registerService(context3.getIComponent(),
        serviceProperties);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

}
