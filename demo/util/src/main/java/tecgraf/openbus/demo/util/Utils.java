package tecgraf.openbus.demo.util;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;

public class Utils {

  /**
   * Busca por uma propriedade dentro da lista de propriedades
   * 
   * @param props a lista de propriedades
   * @param key a chave da propriedade buscada
   * @return o valor da propriedade ou <code>null</code> caso não encontrada
   */
  static public String findProperty(ServiceProperty[] props, String key) {
    for (int i = 0; i < props.length; i++) {
      ServiceProperty property = props[i];
      if (property.name.equals(key)) {
        return property.value;
      }
    }
    return null;
  }

  /**
   * Converte uma cadeia para uma representação textual.
   * 
   * @param chain a cadeia
   * @return uma representação textual da mesma.
   */
  static public String chain2str(CallerChain chain) {
    StringBuffer buffer = new StringBuffer();
    for (LoginInfo loginInfo : chain.originators()) {
      buffer.append(loginInfo.entity);
      buffer.append("->");
    }
    buffer.append(chain.caller().entity);
    return buffer.toString();
  }
}
