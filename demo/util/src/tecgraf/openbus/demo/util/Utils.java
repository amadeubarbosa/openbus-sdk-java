package tecgraf.openbus.demo.util;

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

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

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
      + "  - password = senha (opicional) %s";

  public static final String serverUsage =
    "Usage: 'demo' <host> <port> <entity> <privatekeypath> %s\n"
      + "  - host = é o host do barramento\n"
      + "  - port = é a porta do barramento\n"
      + "  - entity = é a entidade a ser autenticada\n"
      + "  - privatekeypath = é o caminho da chave privada de autenticação da entidade %s";

  public static final String port = "Valor de <port> deve ser um número";
  public static final String keypath =
    "<privatekeypath> deve apontar para uma chave válida.";

  static public String chain2str(LoginInfo[] callers, LoginInfo caller) {
    StringBuffer buffer = new StringBuffer();
    for (LoginInfo loginInfo : callers) {
      buffer.append(loginInfo.entity);
      buffer.append("->");
    }
    buffer.append(caller.entity);
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

  public static void setLogLevel(Level level) {
    Logger logger = Logger.getLogger("tecgraf.openbus");
    logger.setLevel(level);
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(level);
    logger.addHandler(handler);
  }

}
