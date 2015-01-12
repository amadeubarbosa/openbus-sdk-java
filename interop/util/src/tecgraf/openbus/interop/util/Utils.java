package tecgraf.openbus.interop.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;

/**
 * Classe utilitária para os demos Java.
 * 
 * @author Tecgraf
 */
public class Utils {

  /**
   * Lê um arquivo de propriedades.
   * 
   * @param fileName o nome do arquivo.
   * @return as propriedades.
   * @throws IOException
   */
  static public Properties readPropertyFile(String fileName) throws IOException {
    Properties properties = new Properties();
    InputStream propertiesStream = Utils.class.getResourceAsStream(fileName);
    if (propertiesStream == null) {
      throw new FileNotFoundException(String.format(
        "O arquivo de propriedades '%s' não foi encontrado", fileName));
    }
    try {
      properties.load(propertiesStream);
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
    return properties;
  }

  static public String findProperty(ServiceProperty[] props, String key) {
    for (int i = 0; i < props.length; i++) {
      ServiceProperty property = props[i];
      if (property.name.equals(key)) {
        return property.value;
      }
    }
    return null;
  }

  static public String chain2str(CallerChain chain) {
    StringBuffer buffer = new StringBuffer();
    for (LoginInfo loginInfo : chain.originators()) {
      buffer.append(loginInfo.entity);
      buffer.append("->");
    }
    buffer.append(chain.caller().entity);
    return buffer.toString();
  }

  public static Codec getCodec(ORB orb) throws UnknownEncoding, InvalidName {
    org.omg.CORBA.Object obj;
    obj = orb.resolve_initial_references("CodecFactory");
    CodecFactory codecFactory = CodecFactoryHelper.narrow(obj);
    byte major = 1;
    byte minor = 2;
    Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, major, minor);
    return codecFactory.create_codec(encoding);
  }

  public static void setTestLogLevel(Level level) {
    Logger logger = Logger.getLogger("tecgraf.openbus.interop");
    setLevel(logger, level);
  }

  public static void setLibLogLevel(Level level) {
    Logger logger = Logger.getLogger("tecgraf.openbus.core");
    setLevel(logger, level);
  }

  private static void setLevel(Logger logger, Level level) {
    logger.setLevel(level);
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(level);
    logger.addHandler(handler);
  }

  public static class ORBRunThread extends Thread {
    private ORB orb;

    public ORBRunThread(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void run() {
      this.orb.run();
    }
  }

  public static class ShutdownThread extends Thread {
    private ORB orb;
    private List<Connection> conns = new ArrayList<Connection>();
    private List<ServiceOffer> offers = new ArrayList<ServiceOffer>();

    public ShutdownThread(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void run() {

      for (ServiceOffer offer : this.offers) {
        try {
          offer.remove();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }

      for (Connection conn : this.conns) {
        try {
          conn.logout();
        }
        catch (ServiceFailure e) {
          e.printStackTrace();
        }
      }
      this.orb.shutdown(true);
      this.orb.destroy();
    }

    public void addConnetion(Connection conn) {
      this.conns.add(conn);
    }

    public void removeConnetion(Connection conn) {
      this.conns.remove(conn);
    }

    public void addOffer(ServiceOffer offer) {
      this.offers.add(offer);
    }

    public void removeOffer(ServiceOffer offer) {
      this.offers.remove(offer);
    }
  }

  public static List<ServiceOfferDesc> findOffer(OfferRegistry offers,
    ServiceProperty[] search, int count, int tries, int interval)
    throws ServiceFailure {
    List<ServiceOfferDesc> found = new ArrayList<ServiceOfferDesc>();
    for (int i = 0; i < tries; i++) {
      found.clear();
      try {
        Thread.sleep(interval * 1000);
      }
      catch (InterruptedException e1) {
        // continue...
      }
      ServiceOfferDesc[] services = offers.findServices(search);
      if (services.length > 0) {
        for (ServiceOfferDesc offerDesc : services) {
          try {
            if (!offerDesc.service_ref._non_existent()) {
              found.add(offerDesc);
            }
          }
          catch (Exception e) {
            continue;
          }
        }
      }
      if (found.size() >= count) {
        return found;
      }
    }
    StringBuffer buffer = new StringBuffer();
    for (ServiceOfferDesc desc : found) {
      String name = Utils.findProperty(desc.properties, "openbus.offer.entity");
      String login = Utils.findProperty(desc.properties, "openbus.offer.login");
      buffer.append(String.format("\n - %s (%s)", name, login));
    }
    String msg =
      String
        .format(
          "não foi possível encontrar ofertas: found (%d) expected(%d) tries (%d) time (%d)%s",
          found.size(), count, tries, tries * interval, buffer.toString());
    throw new IllegalStateException(msg);
  }

}
