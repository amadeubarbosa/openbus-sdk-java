package demo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.AlreadyLoggedIn;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class DedicatedClockClient {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static String domain;
  private static int interval = 1;
  private static int retries = 10;

  /**
   * Função principal.
   * 
   * @param args argumentos.
   * @throws AlreadyLoggedIn
   * @throws InvalidName
   * @throws ServiceFailure
   */
  public static void main(String[] args) throws AlreadyLoggedIn, InvalidName,
    ServiceFailure {
    // verificando parametros de entrada
    if (args.length < 3) {
      String params = "[interval] [timeout]";
      String desc =
        "\n  - [interval] = Tempo de espera entre tentativas de acesso ao"
          + " barramento em virtude de falhas. Valor padrão é '1'"
          + "\n  - [retries] = Número máximo de tentativas de acesso ao"
          + " barramento em virtude de falhas. Valor padrão é '10'";
      System.out.println(String.format(Usage.clientUsage, params, desc));
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
    // - senha (opcional)
    password = entity;
    if (args.length > 3) {
      password = args[3];
    }
    // - dominio (opcional)
    domain = "openbus";
    if (args.length > 4) {
      domain = args[4];
    }
    // - intervalo entre falhas
    if (args.length > 5) {
      try {
        interval = Integer.parseInt(args[5]);
      }
      catch (NumberFormatException e) {
        System.out.println("Valor de [interval] deve ser um número");
        System.exit(1);
        return;
      }
    }
    // - número máximo de tentativas
    if (args.length > 6) {
      try {
        retries = Integer.parseInt(args[6]);
      }
      catch (NumberFormatException e) {
        System.out.println("Valor de [retries] deve ser um número");
        System.exit(1);
        return;
      }
    }

    // inicializando e configurando o ORB
    ORB orb = ORBInitializer.initORB();
    // recuperando o gerente de contexto de chamadas a barramentos 
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    // conectando ao barramento.
    Connection connection = context.connectByAddress(host, port);
    context.defaultConnection(connection);

    // autentica-se no barramento
    boolean failed;
    do {
      try {
        // autentica-se no barramento
        connection.loginByPassword(entity, password.getBytes(), domain);
        failed = false;
      }
      catch (AlreadyLoggedIn e) {
        // ignorando exceção
        failed = false;
      }
      // login by password
      catch (AccessDenied e) {
        System.err.println(String.format(
          "a senha fornecida para a entidade '%s' foi negada", entity));
        System.exit(1);
        return;
      }
      catch (TooManyAttempts e) {
        System.err.println(String.format(
          "excedeu o limite de tentativas de login. Aguarde %s seg",
          e.penaltyTime));
        System.exit(1);
        return;
      }
      catch (UnknownDomain e) {
        System.err
          .println("Tentativa de autenticação em domínio desconhecido.");
        System.exit(1);
        return;
      }
      // bus core
      catch (ServiceFailure e) {
        failed = true;
        System.err.println(String
          .format("falha severa no barramento em %s:%s : %s", host, port,
            e.message));
      }
      catch (TRANSIENT e) {
        failed = true;
        System.err.println(String.format(
          "o barramento em %s:%s esta inacessível no momento", host, port));
      }
      catch (COMM_FAILURE e) {
        failed = true;
        System.err
          .println("falha de comunicação ao acessar serviços núcleo do barramento");
      }
      catch (NO_PERMISSION e) {
        failed = true;
        if (e.minor == NoLoginCode.value) {
          System.err.println(String.format(
            "não há um login de '%s' válido no momento", entity));
        }
      }
    } while (failed && retry());

    Long timestamp = null;
    do {
      List<RemoteOffer> services;
      try {
        // busca por serviço
        ArrayListMultimap<String, String> properties = ArrayListMultimap
          .create();
        properties.put("offer.domain", "Demo Dedicated Clock");
        services = connection.offerRegistry().findServices(properties);
      }
      // bus core
      catch (ServiceFailure e) {
        System.err.println(String.format(
          "falha severa no barramento em %s:%s : %s", host, port, e.message));
        System.exit(1);
        return;
      }
      catch (TRANSIENT e) {
        System.err.println(String.format(
          "o barramento em %s:%s esta inacessível no momento", host, port));
        System.exit(1);
        return;
      }
      catch (COMM_FAILURE e) {
        System.err
          .println("falha de comunicação ao acessar serviços núcleo do barramento");
        System.exit(1);
        return;
      }
      catch (NO_PERMISSION e) {
        if (e.minor == NoLoginCode.value) {
          System.err.println(String.format(
            "não há um login de '%s' válido no momento", entity));
        }
        System.exit(1);
        return;
      }

      // analisa as ofertas encontradas
      for (RemoteOffer offer : services) {
        try {
          org.omg.CORBA.Object helloObj =
            offer.service().getFacet(ClockHelper.id());
          if (helloObj == null) {
            System.out
              .println("o serviço encontrado não provê a faceta ofertada");
            continue;
          }

          Clock clock = ClockHelper.narrow(helloObj);
          timestamp = clock.getTimeInTicks();
        }
        catch (TRANSIENT e) {
          System.err.println("o serviço encontrado encontra-se indisponível");
        }
        catch (COMM_FAILURE e) {
          System.err.println("falha de comunicação com o serviço encontrado");
        }
        catch (NO_PERMISSION e) {
          switch (e.minor) {
            case NoLoginCode.value:
              System.err.println(String.format(
                "não há um login de '%s' válido no momento", entity));
              break;
            case UnknownBusCode.value:
              System.err
                .println("o serviço encontrado não está mais logado ao barramento");
              break;
            case UnverifiedLoginCode.value:
              System.err
                .println("o serviço encontrado não foi capaz de validar a chamada");
              break;
            case InvalidRemoteCode.value:
              System.err
                .println("integração do serviço encontrado com o barramento está incorreta");
              break;
          }
        }
      }
    } while (timestamp == null && retry());

    // Faz o logout
    context.currentConnection().logout();

    if (timestamp != null) {
      Date date = new Date(timestamp);
      DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
      System.out.println(formatter.format(date));
    }
    else {
      System.out.println("Service is unavailable.");
    }
  }

  public static boolean retry() {
    if (retries > 0) {
      retries--;
      try {
        Thread.sleep(interval * 1000);
      }
      catch (InterruptedException e) {
        // do nothing
      }
      return true;
    }
    return false;
  }
}
