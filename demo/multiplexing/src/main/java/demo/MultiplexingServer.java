package demo;

import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
   * Função principal.
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

    POA poa = context.poa();

    for (int i = 0; i < 3; i++) {
      // criando o serviço a ser ofertado
      ComponentId id =
        new ComponentId("Timer", (byte) 1, (byte) 0, (byte) 0, "java");
      ComponentContext component = new ComponentContext(orb, poa, id);
      TimerImpl timer = new TimerImpl(context);
      component.addFacet("Timer", TimerHelper.id(), timer);
      // conectando ao barramento.
      Connection conn = context.connectByAddress(host, port);
      context.setCurrentConnection(conn);

      // preenche o mapa utilizado pela callback onCallDispatch
      map.put(Arrays.toString(poa.servant_to_id(timer)), conn);
      System.out.println(String.format("Mapa timer %s with id %s", timer
        .hashCode(), Arrays.toString(poa.servant_to_id(timer))));

      // autentica-se no barramento
      boolean failed = true;
      try {
        conn.loginByPrivateKey(entity, privateKey);
        // registrando serviço no barramento
        ArrayListMultimap<String, String> serviceProperties = ArrayListMultimap
          .create();
        serviceProperties.put("offer.domain", "Demo Multiplexing");
        LocalOffer localOffer = conn.offerRegistry().registerService
          (component.getIComponent(), serviceProperties);
        RemoteOffer myOffer = localOffer.remoteOffer(60000, 0);
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
          "a chave em '%s' não corresponde ao certificado da entidade '%s'",
          privateKeyFile, entity));
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
          context.getCurrentConnection().logout();
        }
      }
    }
  }
}
