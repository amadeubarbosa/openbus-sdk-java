package demo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.assistant.AssistantParams;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.exception.AlreadyLoggedIn;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class IndependentClockClient {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static float interval = 1.0f;

  private static AtomicReference<Clock> clock = new AtomicReference<Clock>();
  private static AtomicBoolean searching = new AtomicBoolean(false);
  private static AtomicBoolean shutdown = new AtomicBoolean(false);

  /**
   * Fun��o principal.
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
      String params = "[interval]";
      String desc =
        "\n  - [interval] = Tempo de espera entre tentativas de acesso ao"
          + " barramento em virtude de falhas. Valor padr�o � '1'";
      System.out.println(String.format(Utils.clientUsage, params, desc));
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
    // - senha (opcional)
    password = entity;
    if (args.length > 3) {
      password = args[3];
    }
    // - intervalo entre falhas
    if (args.length > 4) {
      try {
        interval = Float.parseFloat(args[4]);
      }
      catch (NumberFormatException e) {
        System.out.println("Valor de [interval] deve ser um n�mero");
        System.exit(1);
        return;
      }
    }

    // recuperando o assistente
    AssistantParams params = new AssistantParams();
    params.interval = interval;
    final Assistant assist =
      Assistant.createWithPassword(host, port, entity, password.getBytes(),
        params);

    activateSearch(assist);

    // thread cliente independente
    for (int i = 0; i < 20; i++) {
      // recupera valor independente do barramento
      Long timestamp = System.currentTimeMillis();
      Clock aClock = clock.get();
      if (aClock != null) {
        boolean failed = true;
        try {
          // sobrescreve valor com o obtido pelo servi�o
          timestamp = aClock.getTimeInTicks();
          failed = false;
        }
        catch (TRANSIENT e) {
          System.err.println("o servi�o encontrado encontra-se indispon�vel");
        }
        catch (COMM_FAILURE e) {
          System.err.println("falha de comunica��o com o servi�o encontrado");
        }
        catch (NO_PERMISSION e) {
          switch (e.minor) {
            case NoLoginCode.value:
              System.err.println(String.format(
                "n�o h� um login de '%s' v�lido no momento", entity));
              break;
            case UnknownBusCode.value:
              System.err
                .println("o servi�o encontrado n�o est� mais logado ao barramento");
              break;
            case UnverifiedLoginCode.value:
              System.err
                .println("o servi�o encontrado n�o foi capaz de validar a chamada");
              break;
            case InvalidRemoteCode.value:
              System.err
                .println("integra��o do servi�o encontrado com o barramento est� incorreta");
              break;
          }
        }
        finally {
          if (failed) {
            clock.set(null);
            activateSearch(assist);
          }
        }
      }
      Date date = new Date(timestamp);
      DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
      System.out.println(formatter.format(date));
      try {
        Thread.sleep((int) (interval * 1000));
      }
      catch (InterruptedException e) {
        // do nothing
      }
    }

    shutdown.set(true);
    assist.shutdown();
    System.out.println("cliente deslogado...");
  }

  private static void activateSearch(final Assistant assist) {
    if (searching.compareAndSet(false, true)) {
      Thread finder = new Thread() {
        @Override
        public void run() {
          find(assist);
        }
      };
      finder.start();
    }
  }

  private static void find(Assistant assist) {
    clock.set(null);
    do {
      // busca por servi�o
      ServiceProperty[] properties = new ServiceProperty[1];
      properties[0] =
        new ServiceProperty("offer.domain", "Demo Independent Clock");
      ServiceOfferDesc[] services = null;
      try {
        services = assist.findServices(properties, 0);
      }
      // bus core
      catch (ServiceFailure e) {
        System.err.println(String.format(
          "falha severa no barramento em %s:%s : %s", host, port, e.message));
      }
      catch (TRANSIENT e) {
        System.err.println(String.format(
          "o barramento em %s:%s esta inacess�vel no momento", host, port));
      }
      catch (COMM_FAILURE e) {
        System.err
          .println("falha de comunica��o ao acessar servi�os n�cleo do barramento");
      }
      catch (NO_PERMISSION e) {
        if (e.minor == NoLoginCode.value) {
          System.err.println(String.format(
            "n�o h� um login de '%s' v�lido no momento", entity));
        }
      }
      // erros inesperados
      catch (Throwable e) {
        System.err.println("Erro inesperado durante busca de servi�os.");
        e.printStackTrace();
      }

      // analisa as ofertas encontradas
      boolean failed = true;
      if (services != null) {
        for (ServiceOfferDesc offerDesc : services) {
          try {
            org.omg.CORBA.Object helloObj =
              offerDesc.service_ref.getFacet(ClockHelper.id());
            if (helloObj == null) {
              System.out
                .println("o servi�o encontrado n�o prov� a faceta ofertada");
              continue;
            }
            clock.set(ClockHelper.narrow(helloObj));
            failed = false;
            break;
          }
          // Servi�o
          catch (TRANSIENT e) {
            System.err.println("o servi�o encontrado encontra-se indispon�vel");
          }
          catch (COMM_FAILURE e) {
            System.err.println("falha de comunica��o com o servi�o encontrado");
          }
          catch (NO_PERMISSION e) {
            switch (e.minor) {
              case NoLoginCode.value:
                System.err.println(String.format(
                  "n�o h� um login de '%s' v�lido no momento", entity));
                break;
              case UnknownBusCode.value:
                System.err
                  .println("o servi�o encontrado n�o est� mais logado ao barramento");
                break;
              case UnverifiedLoginCode.value:
                System.err
                  .println("o servi�o encontrado n�o foi capaz de validar a chamada");
                break;
              case InvalidRemoteCode.value:
                System.err
                  .println("integra��o do servi�o encontrado com o barramento est� incorreta");
                break;
            }
          }
        }
      }
      if (failed) {
        System.err.println("servi�o esperado n�o foi encontrado.");
        try {
          Thread.sleep((int) (interval * 1000));
        }
        catch (InterruptedException e) {
          // do nothing
        }
      }
    } while (clock.get() == null && !shutdown.get());
    searching.set(false);
  }

}
