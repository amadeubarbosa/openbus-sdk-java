/*
 * $Id$
 */
package tecgraf.openbus.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
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
    this.setUseParentHandlers(false);
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setFormatter(LogFormatter.getInstance());
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
    Log.SERVICES.setLogLevel(newLevel);
    Log.COMMON.setLogLevel(newLevel);
    Log.LEASE.setLogLevel(newLevel);
    Log.INTERCEPTORS.setLogLevel(newLevel);
  }

  /**
   * Define o nível do log.
   * 
   * @param newLevel O novo nível.
   */
  private void setLogLevel(Level newLevel) {
    this.setLevel(newLevel);
    Handler[] handlers = this.getHandlers();
    for (int i = 0; i < handlers.length; i++) {
      handlers[i].setLevel(newLevel);
    }
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
     * A instância única do formatador de registros.
     */
    private static LogFormatter instance;

    /**
     * O formatador de datas.
     */
    private SimpleDateFormat dateFormatter;

    /**
     * Apenas para impedir que seja instanciado externamente.
     */
    private LogFormatter() {
      this.dateFormatter = new SimpleDateFormat("dd/MM HH:mm:ss");
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
      message.append(this.dateFormatter.format(recordDate));
      message.append("\n");
      message.append(this.formatMessage(record));
      message.append("\n");
      Throwable t = record.getThrown();
      if (t != null) {
        message.append("Exceção: ");
        message.append(format(t));
      }
      for (int i = 0; i < 80; i++) {
        message.append("=");
      }
      message.append("\n");
      return message.toString();
    }

    /**
     * Formata a mensagem de uma exceção.
     * 
     * @param t A exceção.
     * 
     * @return A mensagem formatada.
     */
    private static String format(Throwable t) {
      StringBuilder message = new StringBuilder();
      message.append(t.getClass().getName());
      message.append(": ");
      if (t.getMessage() != null) {
        message.append(t.getMessage().trim());
      }
      message.append("\n");
      for (StackTraceElement element : t.getStackTrace()) {
        message.append("==> ");
        message.append(element.getClassName());
        message.append(".");
        message.append(element.getMethodName());
        message.append(" (");
        if (element.isNativeMethod()) {
          message.append("Método Nativo");
        }
        else {
          String fileName = element.getFileName();
          if (fileName == null) {
            message.append("Desconhecido");
          }
          else {
            message.append(fileName);
            int lineNumber = element.getLineNumber();
            if (lineNumber > 0) {
              message.append(":");
              message.append(lineNumber);
            }
          }
        }
        message.append(")\n");
      }
      if (t.getCause() != null && t.getCause() != t) {
        message.append("Causada por: ");
        message.append(format(t.getCause()));
      }
      return message.toString();
    }
  }
}
