package tecgraf.openbus.interop.sharedauth;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.ORBInitializer;
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
    Properties props = Utils.readPropertyFile("/test.properties");
    String iorfile = props.getProperty("bus.ior");
    String path = props.getProperty("sharedauth.file", "sharedauth.dat");
    String entity = "interop_sharedauth_java_client";
    String domain = "testing";
    Utils.setLibLogLevel(Level.parse(props.getProperty("log.lib", "OFF")));

    ORB orb = ORBInitializer.initORB();
    Object busref = orb.string_to_object(Utils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.connectByReference(busref);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET),
      domain);
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
