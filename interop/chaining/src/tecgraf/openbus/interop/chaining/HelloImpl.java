package tecgraf.openbus.interop.chaining;

import java.util.regex.Pattern;

import org.omg.CORBA.NO_PERMISSION;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.interop.simple.HelloPOA;
import tecgraf.openbus.interop.util.Utils;

public class HelloImpl extends HelloPOA {

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
      System.out.println(String.format("aceitando requisição de %s", Utils
        .chain2str(chain)));
      return String.format("Hello %s!", chain.originators()[0].entity);
    }
    else {
      System.out.println(String.format("recusando mensagem de %s", Utils
        .chain2str(chain)));
      throw new NO_PERMISSION();
    }
  }
}
