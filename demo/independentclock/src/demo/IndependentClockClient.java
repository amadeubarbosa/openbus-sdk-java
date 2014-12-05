package demo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
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
  private static int interval = 1;

  /** Variáveis de controle para garantir que não registre réplicas */
  private static ConcurrencyControl options = new ConcurrencyControl();

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
      String params = "[interval]";
      String desc =
        "\n  - [interval] = Tempo de espera entre tentativas de acesso ao"
          + " barramento em virtude de falhas. Valor padrão é '1'";
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
        interval = Integer.parseInt(args[4]);
      }
      catch (NumberFormatException e) {
        System.out.println("Valor de [interval] deve ser um número");
        System.exit(1);
        return;
      }
    }

    // inicializando e configurando o ORB
    ORB orb = ORBInitializer.initORB();
    // recuperando o gerente de contexto de chamadas a barramentos 
    final OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

    Thread client = new Thread() {
      @Override
      public void run() {
        for (int i = 0; i < 10; i++) {
          Long timestamp = null;
          Clock clock = null;
          synchronized (options.lock) {
            clock = options.found;
          }
          if (clock != null) {
            boolean failed = true;
            try {
              timestamp = clock.getTimeInTicks();
              failed = false;
            }
            catch (TRANSIENT e) {
              System.err
                .println("o serviço encontrado encontra-se indisponível");
            }
            catch (COMM_FAILURE e) {
              System.err
                .println("falha de comunicação com o serviço encontrado");
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
            finally {
              if (failed) {
                synchronized (options.lock) {
                  options.found = null;
                }
              }
            }
          }
          if (timestamp == null) {
            synchronized (options.lock) {
              activateSearch(context, interval);
            }
            // recupera valor independente do barramento
            timestamp = System.currentTimeMillis();
          }
          Date date = new Date(timestamp);
          DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
          System.out.println(formatter.format(date));
          try {
            Thread.sleep(interval * 1000);
          }
          catch (InterruptedException e) {
            // do nothing
          }
        }
        try {
          context.getCurrentConnection().logout();
        }
        // bus core
        catch (ServiceFailure e) {
          System.err.println(String.format(
            "falha severa no barramento em %s:%s : %s", host, port, e.message));
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
          System.out.println("cliente deslogado...");
        }
      };
    };
    client.start();

    // conectando ao barramento.
    Connection connection = context.connectByAddress(host, port);
    context.setDefaultConnection(connection);

    connection.onInvalidLoginCallback(new InvalidLoginCallback() {

      @Override
      public void invalidLogin(Connection conn, LoginInfo login) {
        // autentica-se no barramento
        login(conn, entity, password, host, port);
        // buscando serviço no barramento
        activateSearch(context, interval);
      }

      private void login(Connection conn, String entity, String password,
        Object host, Object port) {
        // autentica-se no barramento
        boolean failed;
        do {
          failed = true;
          try {
            // autentica-se no barramento
            conn.loginByPassword(entity, password.getBytes());
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
          }
          catch (TooManyAttempts e) {
            System.err.println(String.format(
              "excedeu o limite de tentativas de login. Aguarde %s seg",
              e.penaltyTime));
            System.exit(1);
            return;
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
      }

    });
    connection.onInvalidLoginCallback().invalidLogin(connection, null);

  }

  private static void activateSearch(final OpenBusContext context,
    final int interval) {
    synchronized (options.lock) {
      if (options.found == null && !options.active) {
        options.active = true;
        Thread finder = new Thread() {
          @Override
          public void run() {
            find(context, interval);
          }
        };
        finder.start();
      }
    }
  }

  private static void find(OpenBusContext context, int interval) {
    do {
      ServiceOfferDesc[] services = null;
      Clock clock = null;
      boolean failed = true;
      try {
        // busca por serviço
        ServiceProperty[] properties = new ServiceProperty[1];
        properties[0] =
          new ServiceProperty("offer.domain", "Demo Independent Clock");
        services = context.getOfferRegistry().findServices(properties);
        failed = false;
      }
      // bus core
      catch (ServiceFailure e) {
        System.err.println(String.format(
          "falha severa no barramento em %s:%s : %s", host, port, e.message));
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
          continue;
        }
      }

      // analisa as ofertas encontradas
      for (ServiceOfferDesc offerDesc : services) {
        try {
          org.omg.CORBA.Object helloObj =
            offerDesc.service_ref.getFacet(ClockHelper.id());
          if (helloObj == null) {
            System.out
              .println("o serviço encontrado não provê a faceta ofertada");
            continue;
          }
          synchronized (options.lock) {
            options.found = ClockHelper.narrow(helloObj);
            clock = options.found;
          }
          break;
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

      if (clock == null) {
        System.err.println("serviço esperado não foi encontrado.");
      }
      else {
        break;
      }
      try {
        Thread.sleep(interval * 1000);
      }
      catch (InterruptedException e) {
        // do nothing
      }
    } while (true);
    synchronized (options.lock) {
      options.active = false;
    }
  }

  public static class ConcurrencyControl {
    public volatile boolean active = false;
    public Clock found = null;
    public Object lock = new Object();
  }

}
