package tecgraf.openbus.interop.sharedauth;

import java.io.File;
import java.io.FileOutputStream;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.interop.util.Configs;
import tecgraf.openbus.interop.util.Utils;
import tecgraf.openbus.security.Cryptography;

/**
 * Demo Single Sign On.
 * 
 * @author Tecgraf
 */
public final class Sharing {

  /**
   * Função main.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String host = configs.bushost;
    int port = configs.busport;
    String entity = "interop_sharedauth_java_client";
    String path = configs.sharedauth;
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();

    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.createConnection(host, port);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET));
    SharedAuthSecret secret = connection.startSharedAuth();
    byte[] encoded = context.encodeSharedAuth(secret);
    File file = new File(path);
    file.createNewFile();
    FileOutputStream fstream = new FileOutputStream(file);
    try {
      fstream.write(encoded);
    }
    finally {
      fstream.close();
    }

    connection.logout();
  }

}
