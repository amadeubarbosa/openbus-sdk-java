package tecgraf.openbus.util;

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
public class Builder {

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

}
