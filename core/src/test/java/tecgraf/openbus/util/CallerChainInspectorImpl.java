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
public class CallerChainInspectorImpl extends CallerChainInspectorPOA {

  /**
   * O Contexto.
   */
  private OpenBusContext context;

  /**
   * Construtor.
   * 
   * @param context o contexto.
   */
  public CallerChainInspectorImpl(OpenBusContext context) {
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] listCallers() {
    CallerChain chain = context.getCallerChain();
    LinkedList<String> list = new LinkedList<String>();
    for (LoginInfo info : chain.originators()) {
      list.add(info.entity);
    }
    list.add(chain.caller().entity);
    return list.toArray(new String[list.size()]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] listCallerLogins() {
    CallerChain chain = context.getCallerChain();
    LinkedList<String> list = new LinkedList<String>();
    for (LoginInfo info : chain.originators()) {
      list.add(info.id);
    }
    list.add(chain.caller().id);
    return list.toArray(new String[list.size()]);
  }

}
