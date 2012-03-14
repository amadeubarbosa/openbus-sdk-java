package tecgraf.openbus.demo.hello;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tecgraf.openbus.Bus;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.BusORBImpl;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.util.Cryptography;

public final class Client {
  public static void main(String[] args) {
    try {
      Logger logger = Logger.getLogger("tecgraf.openbus");
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);
      ConsoleHandler handler = new ConsoleHandler();
      handler.setLevel(Level.INFO);
      logger.addHandler(handler);

      ClientProperties properties = new ClientProperties();

      BusORB orb = new BusORBImpl();
      Bus bus = orb.getBus(properties.getHost(), properties.getPort());
      Connection connection = bus.createConnection();

      connection.loginByPassword(properties.getEntity(), properties
        .getEntityPassword());

      ServiceProperty[] serviceProperties = new ServiceProperty[3];
      serviceProperties[0] =
        new ServiceProperty("openbus.offer.entity", properties
          .getServerEntity());
      serviceProperties[1] =
        new ServiceProperty("openbus.component.facet", "hello");
      serviceProperties[2] =
        new ServiceProperty("offer.domain", "OpenBus Demos");
      ServiceOfferDesc[] services =
        connection.offers().findServices(serviceProperties);

      if (services.length == 1) {
        org.omg.CORBA.Object obj =
          services[0].service_ref.getFacetByName("hello");

        Hello hello = HelloHelper.narrow(obj);
        hello.sayHello();
      }
      else {
        if (services.length == 0) {
          System.err.println("O servidor do demo Hello não foi encontrado");
        }
        else {
          System.err
            .println("Foram encontrados vários servidores do demo Hello");
        }
      }

      connection.logout();

      connection.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static class ClientProperties {
    private Properties properties;

    ClientProperties() throws IOException {
      this.properties = new Properties();
      String propertiesFile = "/Hello.properties";
      InputStream propertiesStream =
        Client.class.getResourceAsStream(propertiesFile);
      if (propertiesStream == null) {
        throw new FileNotFoundException(String.format(
          "O arquivo de propriedades %s não foi encontrado", propertiesFile));
      }
      try {
        this.properties.load(propertiesStream);
      }
      finally {
        try {
          propertiesStream.close();
        }
        catch (IOException e) {
          System.err
            .println("Ocorreu um erro ao fechar o arquivo de propriedades");
          e.printStackTrace();
        }
      }
    }

    String getHost() {
      return this.properties.getProperty("openbus.host.name");
    }

    int getPort() {
      String port = this.properties.getProperty("openbus.host.port");
      return Integer.valueOf(port);
    }

    String getEntity() {
      return this.properties.getProperty("entity.name");
    }

    String getServerEntity() {
      return this.properties.getProperty("server.entity.name");
    }

    byte[] getEntityPassword() {
      String password = this.properties.getProperty("entity.password");
      return password.getBytes(Cryptography.CHARSET);
    }
  }
}
