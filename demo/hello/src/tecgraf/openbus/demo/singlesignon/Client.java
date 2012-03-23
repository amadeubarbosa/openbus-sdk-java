package tecgraf.openbus.demo.singlesignon;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.StandardOpenBus;
import tecgraf.openbus.core.v2_00.OctetSeqHolder;
import tecgraf.openbus.core.v2_00.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.hello.Hello;
import tecgraf.openbus.demo.hello.HelloHelper;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.util.Cryptography;

/**
 * Demo Single Sign On.
 * 
 * @author Tecgraf
 */
public final class Client {

  /**
   * Função main.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) {
    try {
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      Properties properties =
        Utils.readPropertyFile("/singlesignon.properties");
      String host = properties.getProperty("openbus.host.name");
      int port = Integer.valueOf(properties.getProperty("openbus.host.port"));
      String entity = properties.getProperty("entity.name");
      String password = properties.getProperty("entity.password");
      String serverEntity = properties.getProperty("server.entity.name");

      OpenBus openbus = StandardOpenBus.getInstance();
      BusORB orb1 = openbus.initORB();
      BusORB orb2 = openbus.initORB();
      Connection conn1 = openbus.connect(host, port, orb1);
      Connection conn2 = openbus.connect(host, port, orb2);

      conn1.loginByPassword(entity, password.getBytes(Cryptography.CHARSET));
      OctetSeqHolder secret = new OctetSeqHolder();
      LoginProcess process = conn1.startSingleSignOn(secret);

      conn2.loginBySingleSignOn(process, secret.value);

      List<Connection> conns = new ArrayList<Connection>();
      conns.add(conn1);
      conns.add(conn2);

      ServiceProperty[] serviceProperties = new ServiceProperty[3];
      serviceProperties[0] =
        new ServiceProperty("openbus.offer.entity", serverEntity);
      serviceProperties[1] =
        new ServiceProperty("openbus.component.facet", "hello");
      serviceProperties[2] =
        new ServiceProperty("offer.domain", "OpenBus Demos");

      for (Connection conn : conns) {
        ServiceOfferDesc[] services =
          conn.offers().findServices(serviceProperties);
        if (services.length == 0) {
          conn1.logout();
          conn1.close();
          conn2.logout();
          conn2.close();
          throw new IllegalStateException("Não encontrou nenhum serviço Hello.");
        }
        for (ServiceOfferDesc offer : services) {
          org.omg.CORBA.Object obj = offer.service_ref.getFacetByName("hello");
          Hello hello = HelloHelper.narrow(obj);
          hello.sayHello();
        }
      }

      for (Connection conn : conns) {
        conn.logout();
        conn.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
