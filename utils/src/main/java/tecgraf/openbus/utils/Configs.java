package tecgraf.openbus.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Utilitário de configuração de testes
 *
 * @author Tecgraf/PUC-Rio
 */
public class Configs {

  public String bushost;
  public int busport;
  public String busref;
  public String buscrt;
  public String bus2host;
  public int bus2port;
  public String bus2ref;
  public String bus2crt;
  public String admin;
  public byte[] admpsw;
  public String domain;
  public String user;
  public byte[] password;
  public String system;
  public String syskey;
  public String sharedauth;
  public Level testlog;
  public Level log;
  public String orbprops;

  //Java's particular props
  public String wrongkey;
  public String wrongsystem;

  /**
   * Construtor.
   * 
   * @param props propriedades a serem carregadas
   */
  private Configs(Properties props) {
    bushost = props.getProperty("bus.host.name", "localhost");
    busport = Integer.valueOf(props.getProperty("bus.host.port", "2089"));
    busref = props.getProperty("bus.reference.path", "BUS01.ior");
    buscrt = props.getProperty("bus.certificate.path", "BUS01.crt");
    bus2host = props.getProperty("bus2.host.name", bushost);
    Integer port2 = busport + 1;
    bus2port =
      Integer.valueOf(props.getProperty("bus2.host.port", port2.toString()));
    bus2ref = props.getProperty("bus2.reference.path", "BUS02.ior");
    bus2crt = props.getProperty("bus2.certificate.path", "BUS02.crt");

    admin = props.getProperty("admin.enitiy.name", "admin");
    admpsw = props.getProperty("admin.password", admin).getBytes();
    domain = props.getProperty("user.password.domain", "testing");
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

  /**
   * Recupera o arquivo de configurações através da variável de ambiente
   * OPENBUS_TESTCFG, ou do arquivo padrão "/test.properties"
   * 
   * @return as configurações
   * @throws IOException
   */
  public static Configs readConfigsFile() throws IOException {
    String path = System.getenv("OPENBUS_TESTCFG");
    if (path == null) {
      path = "/test.properties";
    }
    return new Configs(Utils.readPropertyFile(path));
  }

  /**
   * Conversão do nível de log de número para o tipo {@link Level}
   * 
   * @param level número do nível de log
   * @return o nível de log
   */
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
