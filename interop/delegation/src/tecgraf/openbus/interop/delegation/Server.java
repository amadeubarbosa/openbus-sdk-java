package tecgraf.openbus.interop.delegation;

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
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.delegation.ForwarderServant.Timer;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.interop.util.Utils.ORBRunThread;
import tecgraf.openbus.interop.util.Utils.ShutdownThread;

public class Server {

  public static void main(String[] args) {
    try {
      Properties props = Utils.readPropertyFile("/delegation.properties");
      String host = props.getProperty("host");
      int port = Integer.valueOf(props.getProperty("port"));

      final ORB orb1 = ORBInitializer.initORB(args);
      new ORBRunThread(orb1).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb1));
      POA poa1 = POAHelper.narrow(orb1.resolve_initial_references("RootPOA"));
      poa1.the_POAManager().activate();

      final ORB orb2 = ORBInitializer.initORB(args);
      new ORBRunThread(orb2).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb2));
      POA poa2 = POAHelper.narrow(orb2.resolve_initial_references("RootPOA"));
      poa2.the_POAManager().activate();

      final ORB orb3 = ORBInitializer.initORB(args);
      new ORBRunThread(orb3).start();
      Runtime.getRuntime().addShutdownHook(new ShutdownThread(orb3));
      POA poa3 = POAHelper.narrow(orb3.resolve_initial_references("RootPOA"));
      poa3.the_POAManager().activate();

      /********** Messenger ***********/
      ConnectionManager connections1 =
        (ConnectionManager) orb1
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn1 = connections1.createConnection(host, port);
      connections1.setDefaultConnection(conn1);
      conn1.onInvalidLoginCallback(new InvalidLoginCallback() {

        @Override
        public void invalidLogin(Connection conn, LoginInfo login, String busid) {
          System.out.println(String.format(
            "login terminated, shutting the server down: %s", login.entity));
          orb1.destroy();
        }
      });

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

      /********** Broadcaster ***********/
      ConnectionManager connections2 =
        (ConnectionManager) orb2
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn2 = connections2.createConnection(host, port);
      connections2.setDefaultConnection(conn2);
      conn2.onInvalidLoginCallback(new InvalidLoginCallback() {

        @Override
        public void invalidLogin(Connection conn, LoginInfo login, String busid) {
          System.out.println(String.format(
            "login terminated, shutting the server down: %s", login.entity));
          orb2.destroy();
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
      ComponentContext context2 =
        new ComponentContext(orb2, poa2, new ComponentId("Broadcaster",
          (byte) 1, (byte) 0, (byte) 0, "java"));
      context2.addFacet(BroadcasterServant.broadcaster, BroadcasterHelper.id(),
        new BroadcasterServant(conn2, conn2messenger));

      conn2.offers().registerService(context2.getIComponent(),
        serviceProperties);

      /********** Forwarder ***********/
      ConnectionManager connections3 =
        (ConnectionManager) orb3
          .resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
      Connection conn3 = connections3.createConnection(host, port);
      connections3.setDefaultConnection(conn3);
      ForwarderServant forwarderServant = new ForwarderServant(conn3);
      ComponentContext context3 =
        new ComponentContext(orb3, poa3, new ComponentId("Forwarder", (byte) 1,
          (byte) 0, (byte) 0, "java"));
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
        public void invalidLogin(Connection conn, LoginInfo login, String busid) {
          timer.stopTimer();
          orb3.destroy();
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
