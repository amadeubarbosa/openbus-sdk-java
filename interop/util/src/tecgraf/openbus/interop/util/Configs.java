package tecgraf.openbus.interop.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Utilitário de configuração dos testes
 *
 * @author Tecgraf/PUC-Rio
 */
public class Configs {

  public String bushost;
  public int busport;
  public String buscrt;
  public String bus2host;
  public int bus2port;
  public String bus2crt;
  public String admin;
  public byte[] admpsw;
  public String user;
  public byte[] password;
  public String system;
  public String syskey;
  public String sharedauth;
  public Level log;
  public Level testlog;
  public String orbprops;

  //Java's particular props
  public String wrongkey;
  public String wrongsystem;

  private Configs(Properties props) {
    bushost = props.getProperty("bus.host.name", "localhost");
    busport = Integer.valueOf(props.getProperty("bus.host.port", "2089"));
    buscrt = props.getProperty("bus.certificate.path", "BUS01.crt");
    bus2host = props.getProperty("bus2.host.name", bushost);
    Integer port2 = busport + 1;
    bus2port =
      Integer.valueOf(props.getProperty("bus2.host.port", port2.toString()));
    bus2crt = props.getProperty("bus2.certificate.path", "BUS02.crt");

    admin = props.getProperty("admin.enitiy.name", "admin");
    admpsw = props.getProperty("admin.password", admin).getBytes();
    user = props.getProperty("user.entity.name", "testuser");
    password = props.getProperty("user.password", user).getBytes();
    system = props.getProperty("system.entity.name", "testsyst");
    syskey = props.getProperty("system.private.key", "testsyst.key");
    sharedauth = props.getProperty("system.sharedauth", "sharedauth.dat");

    testlog =
      parseLevelFromNumber(Integer.valueOf(props.getProperty(
        "openbus.test.verbose", "0")));
    log =
      parseLevelFromNumber(Integer.valueOf(props.getProperty(
        "openbus.log.level", "0")));
    orbprops = props.getProperty("jacorb.properties", "/jacorb.properties");

    wrongkey = props.getProperty("system.wrong.key", "wrong.key");
    wrongsystem = props.getProperty("system.wrong.name", "nocertsyst");
  }

  public static Configs readConfigsFile() throws IOException {
    String path = System.getenv("OPENBUS_TESTCFG");
    if (path == null) {
      path = "/test.properties";
    }
    return new Configs(Utils.readPropertyFile(path));
  }

  private Level parseLevelFromNumber(Integer level) {
    Map<Integer, Level> levels = new HashMap<Integer, Level>();
    levels.put(0, Level.OFF);
    levels.put(1, Level.SEVERE);
    levels.put(2, Level.WARNING);
    levels.put(3, Level.INFO);
    levels.put(4, Level.CONFIG);
    levels.put(5, Level.FINE);
    levels.put(6, Level.FINER);
    levels.put(7, Level.FINEST);
    return levels.get(level);
  }
}
