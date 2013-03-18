package tecgraf.openbus.demo.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;

/**
 * Classe utilitária para os demos Java.
 * 
 * @author Tecgraf
 */
public class Utils {

  public static final String clientUsage =
    "Usage: 'demo' <host> <port> <entity> [password] %s\n"
      + "  - host = é o host do barramento\n"
      + "  - port = é a porta do barramento\n"
      + "  - entity = é a entidade a ser autenticada\n"
      + "  - password = senha (opcional) %s";

  public static final String serverUsage =
    "Usage: 'demo' <host> <port> <entity> <privatekeypath> %s\n"
      + "  - host = é o host do barramento\n"
      + "  - port = é a porta do barramento\n"
      + "  - entity = é a entidade a ser autenticada\n"
      + "  - privatekeypath = é o caminho da chave privada de autenticação da entidade %s";

  public static final String port = "Valor de <port> deve ser um número";
  public static final String keypath =
    "<privatekeypath> deve apontar para uma chave válida.";

  static public String chain2str(CallerChain chain) {
    StringBuffer buffer = new StringBuffer();
    for (LoginInfo loginInfo : chain.originators()) {
      buffer.append(loginInfo.entity);
      buffer.append("->");
    }
    buffer.append(chain.caller().entity);
    return buffer.toString();
  }

  static public String getProperty(ServiceOfferDesc offer, String prop) {
    ServiceProperty[] properties = offer.properties;
    for (int i = 0; i < properties.length; i++) {
      if (properties[i].name.equals(prop)) {
        return properties[i].value;
      }
    }
    return null;
  }

  public static void setLogLevel(Level level) {
    Logger logger = Logger.getLogger("tecgraf.openbus");
    logger.setLevel(level);
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(level);
    logger.addHandler(handler);
  }

  public static void setJacorbLogLevel(Level level) {
    Logger logger = Logger.getLogger("jacorb");
    logger.setLevel(level);
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(level);
    logger.addHandler(handler);
  }

}
