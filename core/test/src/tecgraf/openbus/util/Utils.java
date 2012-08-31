package tecgraf.openbus.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;

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
      throw new FileNotFoundException(String.format(
        "O arquivo de propriedades '%s' n�o foi encontrado", fileName));
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
   * Constr�i componente para o teste de verifica��o de CallerChain dentro de um
   * m�todo de despacho.
   * 
   * @param context o contexto.
   * @return o componente
   * @throws Exception
   */
  public static ComponentContext buildTestCallerChainComponent(
    final OpenBusContext context) throws Exception {
    ComponentContext component = buildComponent(context);
    component.updateFacet("IComponent", new IComponentServant(component) {
      /**
       * M�todo vai lan�ar uma exce��o caso n�o consiga recuperar uma cadeia
       * v�lida. O que far� com que o m�todo de registro de servi�o falhe,
       * fazendo com que o teste tamb�m acuse a falha.
       */
      @Override
      public Object getFacetByName(String arg0) {
        if (context.getCallerChain() == null) {
          throw new IllegalStateException(
            "CallerChain nunca deveria ser nulo dentro de um m�todo de despacho.");
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
   * @throws Exception
   */
  public static ComponentContext buildTestConnectionComponent(
    final OpenBusContext context) throws Exception {
    ComponentContext component = buildComponent(context);
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
   * Constr�i um componente SCS
   * 
   * @param context o contexto
   * @return um componente
   * @throws SCSException
   * @throws AdapterInactive
   * @throws InvalidName
   */
  public static ComponentContext buildComponent(OpenBusContext context)
    throws SCSException, AdapterInactive, InvalidName {
    ORB orb = context.orb();
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    ComponentId id =
      new ComponentId("CallChainTest", (byte) 1, (byte) 0, (byte) 0, "java");
    return new ComponentContext(orb, poa, id);
  }
}
