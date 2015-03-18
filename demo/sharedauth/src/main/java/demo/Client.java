package demo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
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
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.AlreadyLoggedIn;

/**
 * Demo Single Sign On.
 * 
 * @author Tecgraf
 */
public final class Client {

  /**
   * Função main.
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
      String params = "[file]";
      String desc =
        "\n  - [file] = arquivo a ser gerado com informações do compartilhamento de autenticação (opcional)";
      System.out.println(String.format(Usage.clientUsage, params, desc));
      System.exit(1);
      return;
    }
    // - host
    String host = args[0];
    // - porta
    int port;
    try {
      port = Integer.parseInt(args[1]);
    }
    catch (NumberFormatException e) {
      System.out.println(Usage.port);
      System.exit(1);
      return;
    }
    // - entidade
    String entity = args[2];
    // - senha (opcional)
    String password = entity;
    if (args.length > 3) {
      password = args[3];
    }
    // - dominio (opcional)
    String domain = "openbus";
    if (args.length > 4) {
      domain = args[4];
    }
    // - arquivo (opcional)
    String file = "sharedauth.dat";
    if (args.length > 5) {
      file = args[5];
    }

    // inicializando e configurando o ORB
    ORB orb = ORBInitializer.initORB();
    // recuperando o gerente de contexto de chamadas a barramentos 
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    // conectando ao barramento.
    Connection connection = context.connectByAddress(host, port);
    context.setDefaultConnection(connection);

    ServiceOfferDesc[] services;
    try {
      // autentica-se no barramento
      connection.loginByPassword(entity, password.getBytes(), domain);
      // persistindo dados de compartilhamento de autenticação em arquivo
      /*
       * OBS: talvez seja mais interessante para a aplicação trocar esses dados
       * de outra forma. No mínimo, essas informações deveriam ser encriptadas.
       * Além disso, escreveremos apenas uma vez esses dados, que têm validade
       * igual ao lease do login atual. Caso o cliente demore a executar, esses
       * dados não funcionarão, portanto uma outra forma mais dinâmica seria
       * mais eficaz. No entanto, isso foge ao escopo dessa demo.
       */
      SharedAuthSecret secret = connection.startSharedAuth();
      byte[] data = context.encodeSharedAuth(secret);
      FileOutputStream out = null;
      try {
        out = new FileOutputStream(file);
        out.write(data);
      }
      finally {
        if (out != null) {
          out.close();
        }
      }

      // busca por serviço
      ServiceProperty[] properties = new ServiceProperty[1];
      properties[0] = new ServiceProperty("offer.domain", "Demo Hello");
      services = context.getOfferRegistry().findServices(properties);
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
    catch (FileNotFoundException e) {
      System.err.println("Erro ao recuperar arquivo.");
      e.printStackTrace();
      System.exit(1);
      return;
    }
    catch (IOException e) {
      System.err.println("Erro ao escrever dados em arquivo.");
      e.printStackTrace();
      System.exit(1);
      return;
    }

    // analisa as ofertas encontradas
    for (ServiceOfferDesc offerDesc : services) {
      try {
        org.omg.CORBA.Object helloObj =
          offerDesc.service_ref.getFacet(HelloHelper.id());
        if (helloObj == null) {
          System.out
            .println("o serviço encontrado não provê a faceta ofertada");
          continue;
        }

        Hello hello = HelloHelper.narrow(helloObj);
        hello.sayHello();
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
