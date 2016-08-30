package demo;

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
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import demo.GreetingsImpl.Language;
import demo.GreetingsImpl.Period;

import java.util.List;

/**
 * Cliente do demo Greetings
 * 
 * @author Tecgraf
 */
public final class GreetingsClient {

  private static String host;
  private static int port;
  private static String entity;
  private static String password;
  private static Language language = Language.Portuguese;

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
      String params = "[language]";
      String desc =
        "\n  - [language] = deve ser uma das opções: English, Spanish, Portuguese. Padrão é Portuguese";
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
    String domain = "openbus";
    if (args.length > 4) {
      domain = args[4];
    }
    // - language (opcional)
    String lang = "";
    if (args.length > 5) {
      lang = args[5];
    }
    if (lang.equals(Language.Portuguese.name())
      || lang.equals(Language.English.name())
      || lang.equals(Language.Spanish.name())) {
      language = Language.valueOf(lang);
    }

    // inicializando e configurando o ORB
    ORB orb = ORBInitializer.initORB();
    // recuperando o gerente de contexto de chamadas a barramentos 
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    // conectando ao barramento.
    Connection conn = context.connectByAddress(host, port);
    context.setDefaultConnection(conn);

    List<RemoteOffer> services;
    try {
      // autentica-se no barramento
      conn.loginByPassword(entity, password.getBytes(), domain);
      // busca por serviço
      ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
      properties.put("offer.domain", "Demo Greetings");
      properties.put("greetings.language", language.name());
      services = conn.offerRegistry().findServices(properties);
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
      System.err.println("Tentativa de autenticação em domínio desconhecido.");
      System.exit(1);
      return;
    }
    catch (WrongEncoding e) {
      System.err
        .println("incompatibilidade na codifição de informação para o barramento");
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
        for (Period period : Period.values()) {
          String name = "Good" + period.name();
          org.omg.CORBA.Object greetingObj =
            offer.service_ref().getFacetByName(name);
          if (greetingObj == null) {
            System.out.println("o serviço encontrado não provê a faceta "
              + name);
            continue;
          }
          Greetings greetings = GreetingsHelper.narrow(greetingObj);
          System.out.println(greetings.sayGreetings());
        }
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

    // Faz o logout
    context.getCurrentConnection().logout();
  }
}
