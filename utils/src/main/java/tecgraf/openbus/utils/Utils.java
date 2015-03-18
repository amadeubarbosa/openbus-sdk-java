package tecgraf.openbus.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Classe utilit�ria para os demos Java.
 * 
 * @author Tecgraf
 */
public class Utils {

  /**
   * L� um arquivo de propriedades.
   * 
   * @param fileName o nome do arquivo.
   * @return as propriedades.
   * @throws IOException
   */
  static public Properties readPropertyFile(String fileName) throws IOException {
    Properties properties = new Properties();
    InputStream propertiesStream = Utils.class.getResourceAsStream(fileName);
    if (propertiesStream == null) {
      System.err.println(String.format(
        "O arquivo de propriedades '%s' n�o foi encontrado", fileName));
      return properties;
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

  /**
   * L� todo um arquivo e retorna como um array de bytes.
   * 
   * @param path arquivo a ser lido.
   * @return o conte�do do arquivo.
   * @throws IOException
   */
  static public byte[] readFile(String path) throws IOException {
    byte[] data = null;
    File file = new File(path);
    FileInputStream is = new FileInputStream(file);
    try {
      int length = (int) file.length();
      data = new byte[length];
      int offset = is.read(data);
      while (offset < length) {
        int read = is.read(data, offset, length - offset);
        if (read < 0) {
          throw new IOException("N�o foi poss�vel ler todo o arquivo");
        }
        offset += read;
      }
    }
    finally {
      is.close();
    }
    return data;
  }

  /**
   * Configua o n�vel de log dos testes de interoperabilidade
   * 
   * @param level n�vel do log
   */
  public static void setTestLogLevel(Level level) {
    Logger logger = Logger.getLogger("tecgraf.openbus.interop");
    setLogLevel(logger, level);
  }

  /**
   * Configua o n�vel de log da biblioteca de acesso openbus
   * 
   * @param level n�vel do log
   */
  public static void setLibLogLevel(Level level) {
    Logger logger = Logger.getLogger("tecgraf.openbus.core");
    setLogLevel(logger, level);
  }

  /**
   * Configura o n�vel de log
   * 
   * @param logger logger a ser configurado
   * @param level n�vel do log.
   */
  public static void setLogLevel(Logger logger, Level level) {
    logger.setLevel(level);
    for (Handler h : logger.getHandlers()) {
      logger.removeHandler(h);
    }
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(new LogFormatter());
    handler.setLevel(level);
    logger.addHandler(handler);
  }

  /**
   * Formatador de logging
   *
   * @author Tecgraf/PUC-Rio
   */
  private static class LogFormatter extends Formatter {
    /** Formatador de data */
    SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
      String result =
        String.format("%s [%s] %s\n", time.format(record.getMillis()), record
          .getLevel(), record.getMessage());
      Throwable t = record.getThrown();
      return t == null ? result : result + getStackTrace(t);
    }

    /**
     * Convers�o de pilha de erro para {@link String}
     * 
     * @param t o erro.
     * @return a representa��o do erro em {@link String}
     */
    private String getStackTrace(Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      return sw.toString();
    }
  }
}
