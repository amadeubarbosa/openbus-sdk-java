package tecgraf.openbus.interop.sharedauth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.regex.Pattern;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.interop.util.Configs;
import tecgraf.openbus.interop.util.Utils;

public class Consuming {
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String host = configs.bushost;
    int port = configs.busport;
    String path = configs.sharedauth;
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();

    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.createConnection(host, port);
    context.setDefaultConnection(connection);

    byte[] encoded;
    File file = new File(path);
    FileInputStream fstream = null;
    for (int i = 0; i < 10; i++) {
      try {
        fstream = new FileInputStream(file);
      }
      catch (FileNotFoundException e) {
        Thread.sleep(1 * 1000);
      }
    }
    assert fstream != null;
    try {
      encoded = new byte[(int) file.length()];
      int read = fstream.read(encoded);
      assert read == file.length() : "Erro de leitura!";
    }
    finally {
      fstream.close();
      file.delete();
    }
    SharedAuthSecret secret = context.decodeSharedAuth(encoded);
    connection.loginBySharedAuth(secret);

    Pattern pattern =
      Pattern.compile("interop_sharedauth_(java|cpp|lua|csharp)_client");
    LoginInfo login = connection.login();
    assert login != null;
    assert login.id != null;
    assert pattern.matcher(login.entity).matches() : login.entity;

    connection.logout();
  }

}