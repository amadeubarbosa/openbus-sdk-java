package tecgraf.openbus.launcher;

import java.io.InputStream;
import java.util.Properties;

import org.omg.CORBA.ORB;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_06.registry_service.IRegistryService;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.test_case.ServerTestCase;

/**
 * Lança um servidor para fins de teste.
 * 
 * 
 * @author Tecgraf
 */
public class ServerLaucher {

  /**
   * Identificador da oferta.
   */
  private static String registrationId;

  public static void main(String[] args) throws Exception {

    if (args.length < 1) {
      System.err.println("É necessário informar um 'ServerTestCase'.");
      System.exit(1);
    }
    String testCaseName = args[0];

    Object instance = null;
    try {
      instance = Class.forName(testCaseName).newInstance();
    }
    catch (Exception e) {
      System.err.println("Erro ao tentar instanciar a classe " + testCaseName);
      e.printStackTrace();
      System.exit(1);
    }

    if (!(instance instanceof ServerTestCase)) {
      System.err.println(String.format("Classe %s não é do tipo %s",
        testCaseName, ServerTestCase.class.getCanonicalName()));
      System.exit(1);
    }
    final ServerTestCase serverTestCase = (ServerTestCase) instance;

    Openbus openbus = Openbus.getInstance();
    Properties props = new Properties();
    InputStream in =
      ServerLaucher.class.getResourceAsStream("/AllTests.properties");
    if (in != null) {
      try {
        props.load(in);
      }
      finally {
        in.close();
      }
    }
    else {
      System.out
        .println("Erro ao abrir o arquivo de configuração AllTests.properties.");
      System.exit(-1);
    }

    final ServerTestContext context = new ServerTestContext();
    context.properties = props;
    serverTestCase.init(context);

    serverTestCase.connect(context);
    IRegistryService registryService = openbus.getRegistryService();
    if (registryService == null) {
      throw new RSUnavailableException();
    }

    serverTestCase.createComponent(context);

    registrationId = serverTestCase.registerComponent(context);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        Openbus bus = Openbus.getInstance();
        IRegistryService registryService = bus.getRegistryService();
        registryService.unregister(registrationId);

        serverTestCase.disconnect(context);

        serverTestCase.destroy(context);
      }
    });

    System.out.println("Hello Server registrado.");
    ORB orb = openbus.getORB();
    orb.run();
  }
}
