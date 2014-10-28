package demo;

import java.security.interfaces.RSAPrivateKey;

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
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.demo.util.Utils;
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
          + " Valor padrão é '1'";
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
      privateKey = Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    }
    catch (Exception e) {
      System.out.println(Utils.keypath);
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
    final OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

    // criando o serviço a ser ofertado
    // - ativando o POA
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    // - construindo o componente
    ComponentId id =
      new ComponentId("Messenger", (byte) 1, (byte) 0, (byte) 0, "java");
    final ComponentContext component = new ComponentContext(orb, poa, id);
    ProxyMessengerImpl proxy = new ProxyMessengerImpl(context, entity);
    component.addFacet("Messenger", MessengerHelper.id(), proxy);

    // conectando ao barramento.
    Connection conn = context.connectByAddress(host, port);
    context.setDefaultConnection(conn);

    boolean failed = true;
    try {
      // autentica-se no barramento
      conn.loginByCertificate(entity, privateKey);
      // recupera o serviço de registro de ofertas
      OfferRegistry offerRegistry = context.getOfferRegistry();
      // buscando serviço ofertado
      ServiceProperty[] findProperties =
        new ServiceProperty[] {
            new ServiceProperty("offer.role", "actual messenger"),
            new ServiceProperty("offer.domain", "Demo Call Chain"),
            new ServiceProperty("openbus.component.interface", MessengerHelper
              .id()) };
      ServiceOfferDesc[] offers = offerRegistry.findServices(findProperties);
      proxy.setOffers(offers);

      // registrando serviço no barramento
      ServiceProperty[] serviceProperties =
        new ServiceProperty[] {
            new ServiceProperty("offer.role", "proxy messenger"),
            new ServiceProperty("offer.domain", "Demo Call Chain") };

      offerRegistry.registerService(component.getIComponent(),
        serviceProperties);
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
            "a entidade '%s' não foi autorizada pelo administrador do barramento a ofertar os serviços: %s",
            entity, interfaces.toString()));
    }
    catch (InvalidService e) {
      System.err
        .println("o serviço ofertado apresentou alguma falha durante o registro.");
    }
    catch (InvalidProperties e) {
      StringBuffer props = new StringBuffer();
      for (ServiceProperty prop : e.properties) {
        props.append("\n  - ");
        props.append(String.format("name = %s, value = %s", prop.name,
          prop.value));
      }
      System.err.println(String.format(
        "tentativa de registrar serviço com propriedades inválidas: %s", props
          .toString()));
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
        System.exit(1);
      }
    }

  }
}
