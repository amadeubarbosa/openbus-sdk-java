package demo;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.exception.SCSException;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.PrivateKey;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.exception.AlreadyLoggedIn;

/**
 * Parte servidora do demo Hello
 * 
 * @author Tecgraf
 */
public final class DedicatedClockServer {

  private static String host;
  private static int port;
  private static String entity;
  private static OpenBusPrivateKey privateKey;
  private static int interval = 1;

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
      String params = "[interval]";
      String desc =
        "\n  - [interval] = Tempo de espera entre tentativas de acesso ao barramento."
          + " Valor padr�o � '1'";
      System.out.println(String.format(Utils.serverUsage, params, desc));
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
    if (args.length > 4) {
      try {
        interval = Integer.parseInt(args[4]);
      }
      catch (NumberFormatException e) {
        System.out.println("Valor de [interval] deve ser um n�mero");
        System.exit(1);
        return;
      }
    }

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

    // recuperando o gerente de contexto de chamadas � barramentos 
    final OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

    // criando o servi�o a ser ofertado
    // - ativando o POA
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    // - construindo o componente
    ComponentId id =
      new ComponentId("Clock", (byte) 1, (byte) 0, (byte) 0, "java");
    final ComponentContext component = new ComponentContext(orb, poa, id);
    component.addFacet("Clock", ClockHelper.id(), new ClockImpl());

    // conectando ao barramento.
    Connection conn = context.connectByAddress(host, port);
    context.setDefaultConnection(conn);
    conn.onInvalidLoginCallback(new InvalidLoginCallback() {

      /** Vari�veis de controle para garantir que n�o registre r�plicas */
      ConcurrencyControl options = new ConcurrencyControl();

      @Override
      public void invalidLogin(Connection conn, LoginInfo login) {
        synchronized (options.lock) {
          options.disabled = false;
        }
        // autentica-se no barramento
        login(conn, entity, privateKey, host, port, interval);
        // registrando servi�o no barramento
        synchronized (options.lock) {
          if (!options.disabled && !options.active) {
            options.active = true;
            Thread register = new Thread() {
              @Override
              public void run() {
                register(context, component, interval);
              }
            };
            register.start();
          }
        }
      }

      private void login(Connection conn, String entity, PrivateKey privateKey,
        Object host, Object port, int sleepTime) {
        // autentica-se no barramento
        boolean failed;
        do {
          failed = false;
          try {
            conn.loginByCertificate(entity, privateKey);
          }
          catch (AlreadyLoggedIn e) {
            // ignorando exce��o
            failed = false;
          }
          // login by certificate
          catch (AccessDenied e) {
            failed = true;
            System.err
              .println(String
                .format(
                  "a chave utilizada n�o corresponde ao certificado da entidade '%s'",
                  entity));
          }
          catch (MissingCertificate e) {
            failed = true;
            System.err.println(String.format(
              "a entidade %s n�o possui um certificado registrado", entity));
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
              "o barramento em %s:%s esta inacess�vel no momento", host, port));
          }
          catch (COMM_FAILURE e) {
            failed = true;
            System.err
              .println("falha de comunica��o ao acessar servi�os n�cleo do barramento");
          }
          catch (NO_PERMISSION e) {
            failed = true;
            if (e.minor == NoLoginCode.value) {
              System.err.println(String.format(
                "n�o h� um login de '%s' v�lido no momento", entity));
            }
          }
          finally {
            if (failed) {
              try {
                Thread.sleep(sleepTime * 1000);
              }
              catch (InterruptedException e) {
                // do nothing
              }
            }
          }
        } while (failed);
      }

      private void register(OpenBusContext context, ComponentContext component,
        int sleepTime) {
        boolean failed;
        do {
          failed = true;
          try {
            // registrando servi�o no barramento
            ServiceProperty[] serviceProperties = new ServiceProperty[1];
            serviceProperties[0] =
              new ServiceProperty("offer.domain", "Demo Dedicated Clock");
            context.getOfferRegistry().registerService(
              component.getIComponent(), serviceProperties);
            failed = false;
            synchronized (options.lock) {
              options.disabled = true;
            }
          }
          // register
          catch (UnauthorizedFacets e) {
            StringBuffer interfaces = new StringBuffer();
            for (String facet : e.facets) {
              interfaces.append("\n  - ");
              interfaces.append(facet);
            }
            System.err
              .println(String
                .format(
                  "a entidade '%s' n�o foi autorizada pelo administrador do barramento a ofertar os servi�os: %s",
                  entity, interfaces.toString()));
          }
          catch (InvalidService e) {
            System.err
              .println("o servi�o ofertado apresentou alguma falha durante o registro.");
          }
          catch (InvalidProperties e) {
            StringBuffer props = new StringBuffer();
            for (ServiceProperty prop : e.properties) {
              props.append("\n  - ");
              props.append(String.format("name = %s, value = %s", prop.name,
                prop.value));
            }
            System.err.println(String.format(
              "tentativa de registrar servi�o com propriedades inv�lidas: %s",
              props.toString()));
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
          finally {
            if (failed) {
              try {
                Thread.sleep(sleepTime * 1000);
              }
              catch (InterruptedException e) {
                // do nothing
              }
            }
          }
        } while (failed);
        synchronized (options.lock) {
          options.active = false;
        }
      }

    });

    // autentica-se no barramento
    conn.onInvalidLoginCallback().invalidLogin(conn, null);
  }

  public static class ConcurrencyControl {
    public volatile boolean active = false;
    public volatile boolean disabled = false;
    public Object lock = new Object();
  }

}
