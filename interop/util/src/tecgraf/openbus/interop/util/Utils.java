package tecgraf.openbus.interop.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

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

  static public String chain2str(LoginInfo[] callers) {
    StringBuffer buffer = new StringBuffer();
    for (LoginInfo loginInfo : callers) {
      buffer.append(loginInfo.entity);
      buffer.append(";");
    }
    return buffer.toString();
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

    public ShutdownThread(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void run() {
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
  }
}
