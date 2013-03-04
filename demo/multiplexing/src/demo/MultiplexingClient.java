package demo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
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
public final class MultiplexingClient {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static AtomicInteger pending = new AtomicInteger(0);

  /**
   * Fun��o principal.
   * 
   * @param args argumentos.
   * @throws AlreadyLoggedIn
   * @throws InvalidName
   * @throws ServiceFailure
   * @throws AdapterInactive
   */
  public static void main(String[] args) throws AlreadyLoggedIn, InvalidName,
    ServiceFailure, AdapterInactive {
    // verificando parametros de entrada
    if (args.length < 3) {
      System.out.println(String.format(Utils.clientUsage, "", ""));
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

    Utils.setLogLevel(Level.FINEST);

    // inicializando e configurando o ORB
    final ORB orb = ORBInitializer.initORB();
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
        orb.shutdown(true);
        orb.destroy();
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdown);
    // ativando o POA
    final POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();

    // recuperando o gerente de contexto de chamadas a barramentos 
    final OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

    ServiceOfferDesc[] services;
    try {
      // conecta-se ao barramento
      context.setDefaultConnection(newLogin(context));
      // busca por servi�o
      ServiceProperty[] properties =
        new ServiceProperty[] {
            new ServiceProperty("offer.domain", "Demo Multiplexing"),
            new ServiceProperty("openbus.component.interface", TimerHelper.id()) };
      services = context.getOfferRegistry().findServices(properties);
    }
    // login by password
    catch (AccessDenied e) {
      System.err.println(String.format(
        "a senha fornecida para a entidade '%s' foi negada", entity));
      System.exit(1);
      return;
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
        "o barramento em %s:%s esta inacess�vel no momento", host, port));
      System.exit(1);
      return;
    }
    catch (COMM_FAILURE e) {
      System.err
        .println("falha de comunica��o ao acessar servi�os n�cleo do barramento");
      System.exit(1);
      return;
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        System.err.println(String.format(
          "n�o h� um login de '%s' v�lido no momento", entity));
      }
      System.exit(1);
      return;
    }

    // analisa as ofertas encontradas
    for (int i = 0; i < services.length; i++) {
      final int index = i;
      final ServiceOfferDesc offerDesc = services[i];
      // utiliza a oferta em uma thread separada
      new Thread() {
        @Override
        public void run() {
          Connection conn = null;
          try {
            // conecta-se ao barramento
            conn = newLogin(context);
          }
          // login by password
          catch (AccessDenied e) {
            System.err.println(String.format(
              "a senha fornecida para a entidade '%s' foi negada", entity));
            return;
          }
          catch (AlreadyLoggedIn e) {
            // nunca deveria ser lan�ada, mas o Thread.run obriga a captura
            System.err
              .println("tentativa de autenticar uma conex�o j� autenticada");
            return;
          }
          // bus core
          catch (ServiceFailure e) {
            System.err.println(String
              .format("falha severa no barramento em %s:%s : %s", host, port,
                e.message));
            return;
          }
          catch (TRANSIENT e) {
            System.err.println(String.format(
              "o barramento em %s:%s esta inacess�vel no momento", host, port));
            return;
          }
          catch (COMM_FAILURE e) {
            System.err
              .println("falha de comunica��o ao acessar servi�os n�cleo do barramento");
            return;
          }
          catch (NO_PERMISSION e) {
            if (e.minor == NoLoginCode.value) {
              System.err.println(String.format(
                "n�o h� um login de '%s' v�lido no momento", entity));
            }
            return;
          }

          boolean failed = true;
          try {
            context.setCurrentConnection(conn);
            org.omg.CORBA.Object timerObj =
              offerDesc.service_ref.getFacet(TimerHelper.id());
            if (timerObj == null) {
              System.out
                .println("o servi�o encontrado n�o prov� a faceta ofertada");
              return;
            }

            CallbackImpl cb =
              new CallbackImpl(context, conn.login().id, offerDesc);
            poa.servant_to_reference(cb);
            Timer timer = TimerHelper.narrow(timerObj);
            timer.newTrigger(index, cb._this());
            failed = false;
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
          catch (ServantNotActive e) {
            // nunca deveria ser lan�ada, mas o Thread.run obriga a captura
            System.err.println("erro ao ativar o servant");
          }
          catch (WrongPolicy e) {
            // nunca deveria ser lan�ada, mas o Thread.run obriga a captura
            System.err.println("erro ao ativar o servant");
          }
          if (!failed) {
            int pend = pending.incrementAndGet();
            System.out.println("Incrementing pending = " + pend);
          }
          try {
            conn.logout();
          }
          // bus core
          catch (ServiceFailure e) {
            System.err.println(String
              .format("falha severa no barramento em %s:%s : %s", host, port,
                e.message));
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
        };
      }.start();

    }
  }

  private static Connection newLogin(OpenBusContext context)
    throws AccessDenied, AlreadyLoggedIn, ServiceFailure {
    // conectando ao barramento.
    Connection connection = context.createConnection(host, port);
    // autentica-se no barramento
    connection.loginByPassword(entity, password.getBytes());
    return connection;
  }

  public static class CallbackImpl extends CallbackPOA {

    private ServiceOfferDesc offerDesc;
    private String loginId;
    private OpenBusContext context;

    public CallbackImpl(OpenBusContext context, String loginId,
      ServiceOfferDesc offerDesc) {
      this.context = context;
      this.loginId = loginId;
      this.offerDesc = offerDesc;
    }

    @Override
    public void notifyTrigger() {
      CallerChain chain = context.getCallerChain();
      String timerId = Utils.getProperty(offerDesc, "openbus.offer.login");
      if (chain.caller().id.equals(timerId)) {
        System.out.println("notifica��o do timer esperado recebida!");
        if (chain.originators().length > 1
          || !chain.originators()[0].id.equals(loginId)) {
          System.out.println("  notifica��o feita fora da chamada original!");
        }
      }
      else {
        System.out.println("notifica��o inesperada recebida:");
        System.out.println(String
          .format("  recebida de: %s", chain.caller().id));
        System.out.println(String.format("  esperada de: %s", timerId));
      }
      int pend = pending.decrementAndGet();
      System.out.println("decrementing pending = " + pend);
      if (pend == 0) {
        try {
          context.getDefaultConnection().logout();
          // TODO: investigar o BUG
          // ao realizar o shuthwond o jacorb parece estar limpando os slots,
          // gerando uma exce��o minha de INTERNAL_EXCEPTION
          // ORB orb = context.orb();
          // orb.shutdown(false);
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
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

}
