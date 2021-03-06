package demo;

import java.security.interfaces.RSAPrivateKey;
import java.util.List;
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
import tecgraf.openbus.*;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.security.Cryptography;

public class CallChainProxy {
  private static String host;
  private static int port;
  private static String entity;
  private static RSAPrivateKey privateKey;

  public static void main(String[] args) throws InvalidName, AdapterInactive,
    SCSException, AlreadyLoggedIn {
    // verificando parametros de entrada
    if (args.length < 4) {
      String params = "[interval]";
      String desc =
        "\n  - [interval] = Tempo de espera entre tentativas de acesso ao barramento."
          + " Valor padr�o � '1'";
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

    // recuperando o gerente de contexto de chamadas a barramentos 
    final OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

    // criando o servi�o a ser ofertado
    // - ativando o POA
    POA poa = context.POA();
    // - construindo o componente
    ComponentId id =
      new ComponentId("Messenger", (byte) 1, (byte) 0, (byte) 0, "java");
    final ComponentContext component = new ComponentContext(orb, poa, id);
    ProxyMessengerImpl proxy = new ProxyMessengerImpl(context, entity);
    component.addFacet("Messenger", MessengerHelper.id(), proxy);

    // conectando ao barramento.
    Connection conn = context.connectByAddress(host, port);
    context.defaultConnection(conn);

    LocalOffer localOffer;
    boolean failed = true;
    int timeout = 60000;
    try {
      // autentica-se no barramento
      conn.loginByPrivateKey(entity, privateKey);
      // recupera o servi�o de registro de ofertas
      OfferRegistry offerRegistry = conn.offerRegistry();
      // buscando servi�o ofertado
      ArrayListMultimap<String, String> findProperties = ArrayListMultimap
        .create();
      findProperties.put("offer.role", "actual messenger");
      findProperties.put("offer.domain", "Demo Call Chain");
      findProperties.put("openbus.component.interface", MessengerHelper.id());
      List<RemoteOffer> offers = offerRegistry.findServices(findProperties);
      proxy.setOffers(offers);

      // registrando servi�o no barramento
      ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
        .create();
      serviceProperties.put("offer.role", "proxy messenger");
      serviceProperties.put("offer.domain", "Demo Call Chain");
      localOffer = offerRegistry.registerService(component.getIComponent(),
        serviceProperties);
      RemoteOffer myOffer = localOffer.remoteOffer(timeout);
      if (myOffer != null) {
        failed = false;
      } else {
        localOffer.remove();
      }
    }
    // login by certificate
    catch (AccessDenied e) {
      System.err.println(String.format(
        "a chave em '%s' n�o corresponde ao certificado da entidade '%s'",
        privateKeyFile, entity));
    }
    catch (MissingCertificate e) {
      System.err.println(String.format(
        "a entidade %s n�o possui um certificado registrado", entity));
    }
    catch (WrongEncoding e) {
      System.err
        .println("incompatibilidade na codifi��o de informa��o para o barramento");
    }
    // register
    catch (UnauthorizedFacets e) {
      StringBuilder interfaces = new StringBuilder();
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
      StringBuilder props = new StringBuilder();
      for (ServiceProperty prop : e.properties) {
        props.append("\n  - ");
        props.append(String.format("name = %s, value = %s", prop.name,
          prop.value));
      }
      System.err.println(String.format(
        "tentativa de registrar servi�o com propriedades inv�lidas: %s", props
          .toString()));
    }
    catch (TimeoutException e) {
      System.err.println("O servi�o n�o foi registrado em " + timeout +
        " milisegundos.");
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
    } finally {
      if (failed) {
        try {
          context.currentConnection().logout();
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
        System.exit(1);
      }
    }
  }
}
