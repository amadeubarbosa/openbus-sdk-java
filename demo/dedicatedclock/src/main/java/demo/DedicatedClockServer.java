package demo;

import java.security.interfaces.RSAPrivateKey;
import java.util.concurrent.TimeoutException;

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
import tecgraf.openbus.LocalOffer;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.security.Cryptography;

/**
 * Parte servidora do demo Hello
 * 
 * @author Tecgraf
 */
public final class DedicatedClockServer {

  private static String host;
  private static int port;
  private static String entity;
  private static RSAPrivateKey privateKey;
  private static int interval = 1;

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
    SCSException, AlreadyLoggedIn, ServiceFailure, InvalidService,
    TimeoutException, UnauthorizedFacets, InvalidProperties {
    // verificando parametros de entrada
    if (args.length < 4) {
      String params = "[interval]";
      String desc =
        "\n  - [interval] = Tempo de espera entre tentativas de acesso ao barramento."
          + " Valor padrão é '1'";
      System.out.println(String.format(Usage.serverUsage, params, desc));
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
    if (args.length > 4) {
      try {
        interval = Integer.parseInt(args[4]);
      }
      catch (NumberFormatException e) {
        System.out.println("Valor de [interval] deve ser um número");
        System.exit(1);
        return;
      }
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

    // recuperando o gerente de contexto de chamadas à barramentos 
    final OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

    // criando o serviço a ser ofertado
    // - ativando o POA
    POA poa = context.POA();
    // - construindo o componente
    ComponentId id =
      new ComponentId("Clock", (byte) 1, (byte) 0, (byte) 0, "java");
    final ComponentContext component = new ComponentContext(orb, poa, id);
    component.addFacet("Clock", ClockHelper.id(), new ClockImpl());

    // conectando ao barramento.
    Connection conn = context.connectByAddress(host, port);
    context.defaultConnection(conn);

    // autentica-se no barramento
    boolean failed;
    do {
      failed = true;
      try {
        conn.loginByPrivateKey(entity, privateKey);
        failed = false;
      }
      catch (AlreadyLoggedIn e) {
        // ignorando exceção
        failed = false;
      }
      // login by certificate
      catch (AccessDenied e) {
        failed = true;
        System.err
          .println(String
            .format(
              "a chave utilizada não corresponde ao certificado da entidade '%s'",
              entity));
      }
      catch (MissingCertificate e) {
        System.err.println(String.format(
          "a entidade %s não possui um certificado registrado", entity));
      }
      catch (WrongEncoding e) {
        System.err
          .println("incompatibilidade na codifição de informação para o barramento");
      }
      // bus core
      catch (ServiceFailure e) {
        System.err.println(String
          .format("falha severa no barramento em %s:%s : %s", host, port,
            e.message));
      }
      catch (TRANSIENT e) {
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
          try {
            Thread.sleep(interval * 1000);
          }
          catch (InterruptedException e) {
            // do nothing
          }
        }
      }
    } while (failed);

    // registrando serviço no barramento
    ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
      .create();
    serviceProperties.put("offer.domain", "Demo Dedicated Clock");
    LocalOffer localOffer = conn.offerRegistry().registerService(
      component.getIComponent(), serviceProperties);
    RemoteOffer myOffer = localOffer.remoteOffer(60000);
    if (myOffer == null) {
      localOffer.remove();
      System.exit(1);
    }
  }
}

