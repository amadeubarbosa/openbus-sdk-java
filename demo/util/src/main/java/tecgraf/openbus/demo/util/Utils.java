package tecgraf.openbus.demo.util;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

public class Utils {
  /**
   * Converte uma cadeia para uma representação textual.
   * 
   * @param chain a cadeia
   * @return uma representação textual da mesma.
   */
  static public String chain2str(CallerChain chain) {
    StringBuilder buffer = new StringBuilder();
    for (LoginInfo loginInfo : chain.originators()) {
      buffer.append(loginInfo.entity);
      buffer.append("->");
    }
    buffer.append(chain.caller().entity);
    return buffer.toString();
  }
}
