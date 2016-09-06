package tecgraf.openbus.util;

import java.util.LinkedList;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import test.CallerChainInspector;
import test.CallerChainInspectorPOA;

/**
 * Implementação da faceta de {@link CallerChainInspector}
 * 
 * @author Tecgraf
 */
class CallerChainInspectorImpl extends CallerChainInspectorPOA {

  /**
   * O Contexto.
   */
  private final OpenBusContext context;

  /**
   * Construtor.
   * 
   * @param context o contexto.
   */
  public CallerChainInspectorImpl(OpenBusContext context) {
    this.context = context;
  }

  @Override
  public String[] listCallers() {
    CallerChain chain = context.callerChain();
    LinkedList<String> list = new LinkedList<>();
    for (LoginInfo info : chain.originators()) {
      list.add(info.entity);
    }
    list.add(chain.caller().entity);
    return list.toArray(new String[list.size()]);
  }

  @Override
  public String[] listCallerLogins() {
    CallerChain chain = context.callerChain();
    LinkedList<String> list = new LinkedList<>();
    for (LoginInfo info : chain.originators()) {
      list.add(info.id);
    }
    list.add(chain.caller().id);
    return list.toArray(new String[list.size()]);
  }

}
