package tecgraf.openbus.interop.reloggedjoin;

import java.util.List;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.interop.simple.Hello;
import tecgraf.openbus.interop.simple.HelloHelper;
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

  private static final Logger logger = Logger.getLogger(Client.class.getName());

  /**
   * Função principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) throws Exception {
    Configs configs = Configs.readConfigsFile();
    String iorfile = configs.busref;
    String entity = "interop_reloggedjoin_java_client";
    String domain = configs.domain;
    Utils.setTestLogLevel(configs.testlog);
    Utils.setLibLogLevel(configs.log);

    ORB orb = ORBInitializer.initORB();
    Object busref = orb.string_to_object(LibUtils.file2IOR(iorfile));
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection connection = context.connectByReference(busref);
    context.setDefaultConnection(connection);

    connection.loginByPassword(entity, entity.getBytes(Cryptography.CHARSET),
      domain);

    ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
      .create();
    serviceProperties.put("reloggedjoin.role", "proxy");
    serviceProperties.put("offer.domain", "Interoperability Tests");
    List<RemoteOffer> services =
      LibUtils.findOffer(connection.offerRegistry(), serviceProperties, 1, 10,
        1);

    if (services.size() > 1) {
      logger.fine("Foram encontrados vários proxies do demo Hello: "
        + services.size());
    }

    for (RemoteOffer offer : services) {
      String found = offer.properties(false).get("openbus.offer.entity").get(0);
      logger.fine("Entidade encontrada: " + found);
      org.omg.CORBA.Object helloObj = offer.service().getFacetByName
        ("Hello");
      if (helloObj == null) {
        logger.fine("Não foi possível encontrar uma faceta com esse nome.");
        continue;
      }

      Hello hello = HelloHelper.narrow(helloObj);
      if (hello == null) {
        logger.fine("Faceta encontrada não implementa Hello.");
        continue;
      }
      String expected = "Hello " + entity + "!";
      String sayHello = hello.sayHello();
      assert expected.equals(sayHello) : sayHello;
    }
    connection.logout();
  }
}
