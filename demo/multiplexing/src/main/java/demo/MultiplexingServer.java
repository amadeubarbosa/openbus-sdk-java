package demo;

import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

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

/**
 * Parte servidora do demo Hello
 * 
 * @author Tecgraf
 */
public final class MultiplexingServer {

  private static String host;
  private static int port;
  private static String entity;
  private static RSAPrivateKey privateKey;

  /**
   * Fun��o principal.
   * 
   * @param args argumentos.
   * @throws InvalidName
   * @throws AdapterInactive
   * @throws SCSException
   * @throws AlreadyLoggedIn
   * @throws ServiceFailure
   * @throws WrongPolicy
   * @throws ServantNotActive
   */
  public static void main(String[] args) throws InvalidName, AdapterInactive,
    SCSException, AlreadyLoggedIn, ServiceFailure, ServantNotActive,
    WrongPolicy {
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
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");

    final HashMap<String, Connection> map = new HashMap<>();
    context.onCallDispatch((arg0, busid, loginId, object_id, operation) -> {
      System.out.println("dispatch para id: " + Arrays.toString(object_id));
      Connection conn = map.get(Arrays.toString(object_id));
      if (conn != null) {
        System.out.println("achou conn " + conn.login().id);
        return conn;
      }
      else {
        if (!map.isEmpty()) {
          ArrayList<Connection> list = new ArrayList<>(map.values());
          conn = list.get(0);
          System.out.println("aleatorio conn " + conn.login().id);
          return conn;
        }
      }
      return null;
    });

    POA poa = context.POA();

    for (int i = 0; i < 3; i++) {
      // criando o servi�o a ser ofertado
      ComponentId id =
        new ComponentId("Timer", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext component = new ComponentContext(orb, poa, id);
      TimerImpl timer = new TimerImpl(context);
      component.addFacet("Timer", TimerHelper.id(), timer);
      // conectando ao barramento.
      Connection conn = context.connectByAddress(host, port);
      context.currentConnection(conn);

      // preenche o mapa utilizado pela callback onCallDispatch
      map.put(Arrays.toString(poa.servant_to_id(timer)), conn);
      System.out.println(String.format("Mapa timer %s with id %s", timer
        .hashCode(), Arrays.toString(poa.servant_to_id(timer))));

      // autentica-se no barramento
      boolean failed = true;
      int timeout = 60000;
      try {
        conn.loginByPrivateKey(entity, privateKey);
        // registrando servi�o no barramento
        ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
          .create();
        serviceProperties.put("offer.domain", "Demo Multiplexing");
        LocalOffer localOffer = conn.offerRegistry().registerService
          (component.getIComponent(), serviceProperties);
        RemoteOffer myOffer = localOffer.remoteOffer(timeout);
        if (myOffer == null) {
          localOffer.remove();
        } else {
          System.out.println(String.format("Registering timer %s with login %s",
            timer.hashCode(), conn.login().id));
          failed = false;
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
      }
      finally {
        if (failed) {
          context.currentConnection().logout();
        }
      }
    }
  }
}
