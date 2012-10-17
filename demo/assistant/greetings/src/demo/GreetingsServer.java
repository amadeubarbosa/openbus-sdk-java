package demo;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.exception.SCSException;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import demo.GreetingsImpl.Language;
import demo.GreetingsImpl.Period;

/**
 * Parte servidora do demo Hello
 * 
 * @author Tecgraf
 */
public final class GreetingsServer {

  private static String host;
  private static int port;
  private static String entity;
  private static OpenBusPrivateKey privateKey;

  /**
   * Fun��o principal.
   * 
   * @param args argumentos.
   * @throws InvalidName
   * @throws AdapterInactive
   * @throws SCSException
   * @throws AlreadyLoggedIn
   * @throws ServiceFailure
   */
  public static void main(String[] args) throws InvalidName, AdapterInactive,
    SCSException, AlreadyLoggedIn, ServiceFailure {
    // verificando parametros de entrada
    if (args.length < 4) {
      System.out.println(String.format(Utils.serverUsage, "", ""));
      System.exit(1);
      return;
    }
    // - host
    host = args[0];
    // - porta
    try {
      port = Integer.parseInt(args[1]);
    }
    catch (NumberFormatException e) {
      System.out.println(Utils.port);
      System.exit(1);
      return;
    }
    // - entidade
    entity = args[2];
    // - chave privada
    String privateKeyFile = args[3];
    try {
      privateKey = OpenBusPrivateKey.createPrivateKeyFromFile(privateKeyFile);
    }
    catch (Exception e) {
      System.out.println(Utils.keypath);
      e.printStackTrace();
      System.exit(1);
      return;
    }

    // recuperando o assistente
    final Assistant assist =
      Assistant.createWithPrivateKey(host, port, entity, privateKey);
    final ORB orb = assist.orb();
    // - disparando a thread para que o ORB atenda requisi��es
    Thread run = new Thread() {
      @Override
      public void run() {
        orb.run();
      }
    };
    run.start();
    // - criando thread para parar e destruir o ORB ao fim da execu��o do processo 
    Thread shutdown = new Thread() {
      @Override
      public void run() {
        assist.shutdown();
        orb.shutdown(true);
        orb.destroy();
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdown);

    // recuperando o gerente de contexto de chamadas � barramentos 
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    // criando o servi�o a ser ofertado
    // - ativando o POA
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    // - construindo os componentes
    for (Language language : Language.values()) {
      ComponentId id =
        new ComponentId(language.name() + Greetings.class.getSimpleName(),
          (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext component = new ComponentContext(orb, poa, id);
      for (Period period : Period.values()) {
        component.addFacet("Good" + period.name(), GreetingsHelper.id(),
          new GreetingsImpl(context, language, period));
      }
      // registrando servi�o no barramento
      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      serviceProperties[0] =
        new ServiceProperty("offer.domain", "Demo Greetings");
      serviceProperties[1] =
        new ServiceProperty("greetings.language", language.name());
      assist.registerService(component.getIComponent(), serviceProperties);
    }

  }
}
