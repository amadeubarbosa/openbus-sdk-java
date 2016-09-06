package demo;

import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.exception.SCSException;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.security.Cryptography;
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
  private static RSAPrivateKey privateKey;

  /**
   * Função principal.
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
      System.out.println(String.format(Usage.serverUsage, "", ""));
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
      System.out.println(Usage.port);
      System.exit(1);
      return;
    }
    // - entidade
    entity = args[2];
    // - chave privada
    String privateKeyFile = args[3];
    try {
      privateKey = Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    }
    catch (Exception e) {
      System.out.println(Usage.keypath);
      e.printStackTrace();
      System.exit(1);
      return;
    }

    // inicializando e configurando o ORB
    final ORB orb = ORBInitializer.initORB();
    // - disparando a thread para que o ORB atenda requisições
    Thread run = new Thread() {
      @Override
      public void run() {
        orb.run();
      }
    };
    run.start();
    // - criando thread para parar e destruir o ORB ao fim da execução do processo 
    Thread shutdown = new Thread() {
      @Override
      public void run() {
        orb.shutdown(true);
        orb.destroy();
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdown);

    // recuperando o gerente de contexto de chamadas a barramentos 
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    POA poa = context.POA();

    // criando o serviço a ser ofertado
    // - construindo os componentes
    HashMap<Language, ComponentContext> components = new HashMap<>();
    for (Language language : Language.values()) {
      ComponentId id =
        new ComponentId(language.name() + Greetings.class.getSimpleName(),
          (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext component = new ComponentContext(orb, poa, id);
      for (Period period : Period.values()) {
        component.addFacet("Good" + period.name(), GreetingsHelper.id(),
          new GreetingsImpl(context, language, period));
      }
      components.put(language, component);
    }

    // conectando ao barramento.
    Connection conn = context.connectByAddress(host, port);
    context.defaultConnection(conn);

    // autentica-se no barramento
    boolean failed = true;
    try {
      conn.loginByPrivateKey(entity, privateKey);
      // registrando serviço no barramento
      for (Entry<Language, ComponentContext> entry : components.entrySet()) {
        ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
          .create();
        serviceProperties.put("offer.domain", "Demo Greetings");
        serviceProperties.put("greetings.language", entry.getKey().name());
        conn.offerRegistry().registerService(entry.getValue().getIComponent(),
          serviceProperties);
      }
      failed = false;
    }
    // login by certificate
    catch (AccessDenied e) {
      System.err.println(String.format(
        "a chave em '%s' não corresponde ao certificado da entidade '%s'",
        privateKeyFile, entity));
    }
    catch (MissingCertificate e) {
      System.err.println(String.format(
        "a entidade %s não possui um certificado registrado", entity));
    }
    // bus core
    catch (ServiceFailure e) {
      System.err.println(String.format(
        "falha severa no barramento em %s:%s : %s", host, port, e.message));
    }
    catch (TRANSIENT e) {
      failed = true;
      System.err.println(String.format(
        "o barramento em %s:%s esta inacessível no momento", host, port));
    }
    catch (COMM_FAILURE e) {
      System.err
        .println("falha de comunicação ao acessar serviços núcleo do barramento");
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        System.err.println(String.format(
          "não há um login de '%s' válido no momento", entity));
      }
    }
    finally {
      if (failed) {
        context.currentConnection().logout();
        System.exit(1);
      }
    }
  }
}
