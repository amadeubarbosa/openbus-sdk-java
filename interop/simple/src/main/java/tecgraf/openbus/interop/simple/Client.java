package tecgraf.openbus.interop.simple;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.security.Cryptography;
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
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.busref;
    String entity = "interop_hello_java_client";
    String domain = configs.domain;
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();
    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.connectByReference(busref);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET),
      domain);

    ArrayListMultimap<String, String> props = ArrayListMultimap.create();
    props.put("offer.domain", "Interoperability Tests");
    props.put("openbus.component.interface", HelloHelper.id());
    List<RemoteOffer> services =
      LibUtils.findOffer(connection.offerRegistry(), props, 1, 10, 1);

    for (RemoteOffer offer : services) {
      String found = offer.properties(false).get("openbus.offer.entity").get(0);
      org.omg.CORBA.Object helloObj = offer.service_ref().getFacetByName
        ("Hello");
      if (helloObj == null) {
        continue;
      }

      Hello hello = HelloHelper.narrow(helloObj);
      if (hello == null) {
        continue;
      }
      String expected = "Hello " + entity + "!";
      String sayHello = hello.sayHello();
      assert expected.equals(sayHello) : String.format("Entidade (%s): %s",
        found, sayHello);
    }

    connection.logout();
    orb.shutdown(true);
    orb.destroy();
  }
}
