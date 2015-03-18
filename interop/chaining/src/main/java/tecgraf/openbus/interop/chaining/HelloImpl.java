package tecgraf.openbus.interop.chaining;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.omg.CORBA.NO_PERMISSION;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.interop.simple.HelloPOA;
import tecgraf.openbus.utils.LibUtils;

public class HelloImpl extends HelloPOA {

  private static final Logger logger = Logger.getLogger(HelloImpl.class
    .getName());
  private OpenBusContext context;
  private Pattern pattern;

  public HelloImpl(OpenBusContext context) {
    this.context = context;
    this.pattern =
      Pattern.compile("interop_chaining_(cpp|java|lua|csharp)_proxy");
  }

  @Override
  public String sayHello() {
    CallerChain chain = context.getCallerChain();
    if (this.pattern.matcher(chain.caller().entity).matches()) {
      logger.fine(String.format("aceitando requisição de %s", LibUtils
        .chain2str(chain)));
      return String.format("Hello %s!", chain.originators()[0].entity);
    }
    else {
      logger.fine(String.format("recusando mensagem de %s", LibUtils
        .chain2str(chain)));
      throw new NO_PERMISSION();
    }
  }
}
