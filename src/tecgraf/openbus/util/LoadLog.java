package tecgraf.openbus.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoadLog extends Logger {

  /**
   * Cria um LoadLog.
   * 
   * @param name O nome do LoadLog.
   */
  public LoadLog(String name) {
    super(name, null);
    this.setUseParentHandlers(false);
    Handler handler = null;
    try {
      File dir = new File("testOutput");
      if (!dir.exists()) {
        if (!dir.mkdir()) {
          System.out.println("Erro ao criar diretorio!");
          handler = new ConsoleHandler();
          handler.setFormatter(LoadLogFormatter.getInstance());
          this.addHandler(handler);
        }
      }
      if (handler == null) {
        handler =
          new FileHandler("testOutput/OpenbusLog.txt", 10000000, 5, true);
        handler.setFormatter(LoadLogFormatter.getInstance());
        this.addHandler(handler);
      }
    }
    catch (SecurityException e) {
      e.printStackTrace();
      handler = new ConsoleHandler();
      handler.setFormatter(LoadLogFormatter.getInstance());
      this.addHandler(handler);
    }
    catch (IOException e) {
      e.printStackTrace();
      handler = new ConsoleHandler();
      handler.setFormatter(LoadLogFormatter.getInstance());
      this.addHandler(handler);
    }

  }

  /**
   * Formatador de registros do log de teste de carga.
   * 
   * @author Tecgraf/PUC-Rio
   */
  private static class LoadLogFormatter extends Formatter {

    /**
     * A instância única do formatador de registros.
     */
    private static LoadLogFormatter instance;

    /**
     * O formatador de datas.
     */
    private SimpleDateFormat dateFormatter =
      new SimpleDateFormat("dd/MM; HH:mm:ss");

    /**
     * Apenas para impedir que seja instanciado externamente.
     */
    private LoadLogFormatter() {
    }

    /**
     * Obtém a instância única do formatador de registros.
     * 
     * @return A instância única do formatador de registros.
     */
    static LoadLogFormatter getInstance() {
      if (instance == null) {
        instance = new LoadLogFormatter();
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
      message.append("; ");
      message.append(this.formatMessage(record));
      message.append("; ");
      Throwable t = record.getThrown();
      if (t != null) {
        message.append("Exceção: ");
        message.append(format(t));
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
