package tecgraf.openbus.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
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

  /**
   * Constrói componente para o teste de verificação de CallerChain dentro de um
   * método de despacho.
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
       * Método vai lançar uma exceção caso não consiga recuperar uma cadeia
       * válida. O que fará com que o método de registro de serviço falhe,
       * fazendo com que o teste também acuse a falha.
       */
      @Override
      public Object getFacetByName(String arg0) {
        Connection connection = context.getCurrentConnection();
        CallerChain chain = context.getCallerChain();
        if (chain == null) {
          throw new IllegalStateException(
            "CallerChain nunca deveria ser nulo dentro de um método de despacho.");
        }
        // verificando dados da cadeia
        if (!connection.busid().equals(chain.busid())) {
          throw new IllegalStateException(
            "Informação de busId da cadeia não é coerente com conexão que atende a requisição.");
        }
        if (chain.caller() == null || chain.caller().entity == null
          || chain.caller().id == null) {
          throw new IllegalStateException(
            "Informação de caller da cadeia é inválida.");
        }
        if (chain.target() == null
          || !connection.login().entity.equals(chain.target())) {
          throw new IllegalStateException(
            "Informação de target da cadeia não é coerente com conexão que atende a requisição..");
        }
        return super.getFacetByName(arg0);
      }
    });
    return component;
  }

  /**
   * Constrói componente para o teste de verificação de CallerChain dentro de um
   * método de despacho.
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
       * Método vai lançar uma exceção caso não consiga recuperar uma conexão. O
       * que fará com que o método de registro de serviço falhe, fazendo com que
       * o teste também acuse a falha.
       */
      @Override
      public Object getFacetByName(String arg0) {
        Connection connection = context.getCurrentConnection();
        if (connection == null) {
          throw new IllegalStateException(
            "CurrentConnection nunca deveria ser nulo dentro de um método de despacho.");
        }
        return super.getFacetByName(arg0);
      }
    });
    return component;
  }

  /**
   * Constrói um componente que oferece faceta de inspeção de cadeia de
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
   * Constrói um componente SCS
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
   * Configura o nível de log
   * 
   * @param level nível do log.
   */
  public static void setLogLevel(Level level) {
    Logger logger = Logger.getLogger("tecgraf.openbus");
    logger.setLevel(level);
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(level);
    logger.addHandler(handler);
  }
}
