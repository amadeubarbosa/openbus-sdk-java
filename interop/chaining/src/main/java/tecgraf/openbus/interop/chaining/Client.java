package tecgraf.openbus.interop.chaining;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.LibUtils;
import tecgraf.openbus.utils.Utils;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class Client {
  /**
   * Função principal.
   * 
   * @param args argumentos.
   * @throws AlreadyLoggedIn
   * @throws InvalidName
   * @throws ServiceFailure
   * @throws AccessDenied
   * @throws IOException
   * @throws TooManyAttempts
   * @throws UnknownDomain
   * @throws WrongEncoding
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.busref;
    String entity = "interop_chaining_java_client";
    String domain = configs.domain;
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB(args);
    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.connectByReference(busref);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(), domain);
    ArrayListMultimap<String, String> props = ArrayListMultimap.create();
    props.put("offer.domain", "Interoperability Tests");
    props.put("openbus.component.interface", HelloProxyHelper.id());
    List<RemoteOffer> services =
      LibUtils.findOffer(connection.offerRegistry(), props, 1, 10, 1);

    for (RemoteOffer offer : services) {
      org.omg.CORBA.Object msgObj =
        offer.service_ref().getFacet(HelloProxyHelper.id());
      if (msgObj == null) {
        continue;
      }
      String destination = offer.properties(false).get(
        "openbus.offer.entity").get(0);
      CallerChain chain = connection.makeChainFor(destination);
      byte[] encodedChain = context.encodeChain(chain);
      HelloProxy proxy = HelloProxyHelper.narrow(msgObj);
      String sayHello = proxy.fetchHello(encodedChain);

      String expected = "Hello " + entity + "!";
      assert expected.equals(sayHello) : sayHello;
    }
    context.getCurrentConnection().logout();
  }
}
