package tecgraf.openbus.util;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

public class Configs {

  public String host;
  public int port;
  public String busref;
  public String admin;
  public byte[] admpsw;
  public String domain;
  public String user;
  public byte[] password;
  public String system;
  public String syskey;
  public Level log;
  public String orbprops;

  //Java's particular props
  public String wrongkey;
  public String wrongsystem;

  private Configs(Properties props) {
    host = props.getProperty("bus.host.name", "localhost");
    port = Integer.valueOf(props.getProperty("bus.host.port", "2089"));
    busref = props.getProperty("bus.reference.path", "BUS01.ior");
    admin = props.getProperty("admin.enitiy.name", "admin");
    admpsw = props.getProperty("admin.password", admin).getBytes();
    domain = props.getProperty("user.password.domain", "testing");
    user = props.getProperty("user.entity.name", "testuser");
    password = props.getProperty("user.password", user).getBytes();
    system = props.getProperty("system.entity.name", "testsyst");
    syskey = props.getProperty("system.private.key", "testsyst.key");
    log = Level.parse(props.getProperty("openbus.log.level", "OFF"));
    orbprops = props.getProperty("jacorb.properties", "/jacorb.properties");

    wrongkey = props.getProperty("system.wrong.key", "wrong.key");
    wrongsystem = props.getProperty("system.wrong.name", "nocertsyst");
  }

  public static Configs readConfigsFile(String path) throws IOException {
    return new Configs(Utils.readPropertyFile(path));
  }
}
