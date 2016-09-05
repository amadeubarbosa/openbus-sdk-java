package tecgraf.openbus.interop.sharedauth;

import java.io.File;
import java.io.FileOutputStream;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.Utils;

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
    String iorfile = configs.busref;
    String path = configs.sharedauth;
    String entity = "interop_sharedauth_java_client";
    String domain = configs.domain;
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();
    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.connectByReference(busref);
    context.defaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET),
      domain);
    SharedAuthSecret secret = connection.startSharedAuth();
    byte[] encoded = context.encodeSharedAuth(secret);
    File file = new File(path);
    if (!file.createNewFile()) {
      assert file.delete();
      assert file.createNewFile();
    }
    try (FileOutputStream fstream = new FileOutputStream(file)) {
      fstream.write(encoded);
    }
    connection.logout();
  }
}
