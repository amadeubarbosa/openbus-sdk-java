package tecgraf.openbus.util;

import static org.junit.Assert.fail;

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

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponentServant;
import scs.core.exception.SCSException;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import test.CallerChainInspectorHelper;

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
          fail("N�o foi poss�vel ler todo o arquivo");
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
   * Constr�i componente para o teste de verifica��o de CallerChain dentro de um
   * m�todo de despacho.
   * 
   * @param context o contexto.
   * @return o componente
   * @throws SCSException
   * @throws InvalidName
   * @throws AdapterInactive
   */
  public static ComponentContext buildTestCallerChainComponent(
    final OpenBusContext context) throws AdapterInactive, InvalidName,
    SCSException {
    ComponentContext component = buildComponent(context.orb());
    component.updateFacet("IComponent", new IComponentServant(component) {
      /**
       * M�todo vai lan�ar uma exce��o caso n�o consiga recuperar uma cadeia
       * v�lida. O que far� com que o m�todo de registro de servi�o falhe,
       * fazendo com que o teste tamb�m acuse a falha.
       */
      @Override
      public Object getFacetByName(String arg0) {
        Connection connection = context.getCurrentConnection();
        CallerChain chain = context.getCallerChain();
        if (chain == null) {
          throw new IllegalStateException(
            "CallerChain nunca deveria ser nulo dentro de um m�todo de despacho.");
        }
        // verificando dados da cadeia
        if (!connection.busid().equals(chain.busid())) {
          throw new IllegalStateException(
            "Informa��o de busId da cadeia n�o � coerente com conex�o que atende a requisi��o.");
        }
        if (chain.caller() == null || chain.caller().entity == null
          || chain.caller().id == null) {
          throw new IllegalStateException(
            "Informa��o de caller da cadeia � inv�lida.");
        }
        if (chain.target() == null
          || !connection.login().entity.equals(chain.target())) {
          throw new IllegalStateException(
            "Informa��o de target da cadeia n�o � coerente com conex�o que atende a requisi��o..");
        }
        return super.getFacetByName(arg0);
      }
    });
    return component;
  }

  /**
   * Constr�i componente para o teste de verifica��o de CallerChain dentro de um
   * m�todo de despacho.
   * 
   * @param context o contexto.
   * @return o componente
   * @throws SCSException
   * @throws InvalidName
   * @throws AdapterInactive
   */
  public static ComponentContext buildTestConnectionComponent(
    final OpenBusContext context) throws AdapterInactive, InvalidName,
    SCSException {
    ComponentContext component = buildComponent(context.orb());
    component.updateFacet("IComponent", new IComponentServant(component) {
      /**
       * M�todo vai lan�ar uma exce��o caso n�o consiga recuperar uma conex�o. O
       * que far� com que o m�todo de registro de servi�o falhe, fazendo com que
       * o teste tamb�m acuse a falha.
       */
      @Override
      public Object getFacetByName(String arg0) {
        Connection connection = context.getCurrentConnection();
        if (connection == null) {
          throw new IllegalStateException(
            "CurrentConnection nunca deveria ser nulo dentro de um m�todo de despacho.");
        }
        return super.getFacetByName(arg0);
      }
    });
    return component;
  }

  /**
   * Constr�i um componente que oferece faceta de inspe��o de cadeia de
   * chamadas.
   * 
   * @param context o contexto
   * @return o componente
   * @throws AdapterInactive
   * @throws InvalidName
   * @throws SCSException
   */
  public static ComponentContext buildTestCallerChainInspectorComponent(
    final OpenBusContext context) throws AdapterInactive, InvalidName,
    SCSException {
    ComponentContext component = buildComponent(context.orb());
    component.addFacet("CallerChainInspector", CallerChainInspectorHelper.id(),
      new CallerChainInspectorImpl(context));
    return component;
  }

  /**
   * Constr�i um componente SCS
   * 
   * @param orb o orb em uso
   * @return um componente
   * @throws SCSException
   * @throws AdapterInactive
   * @throws InvalidName
   */
  public static ComponentContext buildComponent(ORB orb) throws SCSException,
    AdapterInactive, InvalidName {
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    ComponentId id =
      new ComponentId("TestComponent", (byte) 1, (byte) 0, (byte) 0, "java");
    return new ComponentContext(orb, poa, id);
  }

  /**
   * Configura o n�vel de log
   * 
   * @param level n�vel do log.
   */
  public static void setLogLevel(Level level) {
    Logger logger = Logger.getLogger("tecgraf.openbus");
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

  private static class LogFormatter extends Formatter {
    SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
      String result =
        String.format("%s [%s] %s\n", time.format(record.getMillis()), record
          .getLevel(), record.getMessage());
      Throwable t = record.getThrown();
      return t == null ? result : result + getStackTrace(t);
    }

    private String getStackTrace(Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      return sw.toString();
    }
  }
}
