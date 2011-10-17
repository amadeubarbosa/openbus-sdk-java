package tecgraf.openbus.demo.hello;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tecgraf.openbus.AlreadyLoggedException;
import tecgraf.openbus.Bus;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.CryptographyException;
import tecgraf.openbus.InternalException;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_00.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_00.services.offer_registry.ServiceProperty;
import tecgraf.openbus.defaultimpl.BusORBImpl;

public final class Client {
  public static void main(String[] args) throws IOException, InternalException,
    AlreadyLoggedException, CryptographyException, AccessDenied, WrongEncoding,
    ServiceFailure {
    Logger logger = Logger.getLogger("tecgraf.openbus");
    logger.setLevel(Level.FINEST);
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(Level.FINEST);
    logger.addHandler(handler);

    ClientProperties properties = new ClientProperties();

    BusORB orb = new BusORBImpl();
    Bus bus = orb.getBus(properties.getHost(), properties.getPort());
    Connection connection = bus.createConnection();

    connection.loginByPassword(properties.getEntity(), properties
      .getEntityPassword());

    ServiceProperty[] serviceProperties = new ServiceProperty[3];
    serviceProperties[0] = new ServiceProperty("openbus.offer.entity", "demo");
    serviceProperties[1] =
      new ServiceProperty("openbus.component.facet", "hello");
    serviceProperties[2] = new ServiceProperty("offer.domain", "OpenBus Demos");
    ServiceOfferDesc[] services =
      connection.getOffers().findServices(serviceProperties);

    if (services.length == 0) {
      System.out.println("O servidor do demo Hello não foi encontrado");
    }
    else {
      org.omg.CORBA.Object obj =
        services[0].service_ref.getFacetByName("hello");
      IHello hello = IHelloHelper.narrow(obj);
      hello.sayHello();
    }

    connection.logout();

    connection.close();
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

    char[] getEntityPassword() {
      String password = this.properties.getProperty("entity.password");
      return password.toCharArray();
    }
  }
}
