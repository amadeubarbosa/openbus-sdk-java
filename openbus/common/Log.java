/*
 * $Id$
 */
package openbus.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Mecanismo de log do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Log extends Logger {
  /**
   * O log utilizado pelos serviços.
   */
  public static final Log SERVICES = new Log("openbus.services");

  /**
   * O log dos mecanismos comuns.
   */
  public static final Log COMMON = new Log("openbus.common");

  /**
   * O log do mecanismo de lease.
   */
  public static final Log LEASE = new Log("openbus.common.lease");

  /**
   * O log do mecanismo de interceptadores.
   */
  public static final Log INTERCEPTORS = new Log("openbus.common.interceptors");

  /**
   * Cria um log.
   * 
   * @param name O nome do log.
   */
  private Log(String name) {
    super(name, null);
    this.setLevel(Level.FINE);
    this.setUseParentHandlers(false);
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setFormatter(LogFormatter.getInstance());
    consoleHandler.setLevel(Level.FINE);
    this.addHandler(consoleHandler);
  }

  /**
   * Define o nível dos logs do OpenBus.
   * 
   * @param newLevel O nível.
   * 
   * @see #setLevel(Level)
   */
  public static void setLogsLevel(Level newLevel) {
    Log.SERVICES.setLevel(newLevel);
    Log.COMMON.setLevel(newLevel);
    Log.LEASE.setLevel(newLevel);
    Log.INTERCEPTORS.setLevel(newLevel);
  }

  /**
   * Registra uma mensagem severa.
   * 
   * @param msg A mensagem.
   * @param thrown A exceção associada à mensagem.
   */
  public void severe(String msg, Throwable thrown) {
    this.log(Level.SEVERE, msg, thrown);
  }

  /**
   * Formatador de registros do log.
   * 
   * @author Tecgraf/PUC-Rio
   */
  private static class LogFormatter extends Formatter {
    /**
     * O separador dos campos do registro.
     */
    private static final String SEPARATOR = " - ";

    /**
     * A instância única do formatador de registros.
     */
    static LogFormatter instance;

    /**
     * Apenas para impedir que seja instanciado externamente.
     */
    private LogFormatter() {
    }

    /**
     * Obtém a instância única do formatador de registros.
     * 
     * @return A instância única do formatador de registros.
     */
    static LogFormatter getInstance() {
      if (instance == null) {
        instance = new LogFormatter();
      }
      return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String format(LogRecord record) {
      StringBuilder message = new StringBuilder();
      message.append("[");
      message.append(record.getLoggerName());
      message.append(" ");
      message.append(record.getLevel());
      message.append("]");
      message.append(" ");
      Date recordDate = new Date(record.getMillis());
      SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM HH:mm:ss");
      message.append(dateFormatter.format(recordDate));
      message.append(SEPARATOR);
      message.append(this.formatMessage(record));
      message.append("\n");
      message.append("Classe: ");
      message.append(record.getSourceClassName());
      message.append(SEPARATOR);
      message.append("Método: ");
      message.append(record.getSourceMethodName());
      message.append("\n\n");
      return message.toString();
    }
  }
}