package demo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcessHelper;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidLoginProcess;

public class SharedAuthClient {
  public static void main(String[] args) throws ServiceFailure, InvalidName,
    AlreadyLoggedIn {
    String help =
      "Usage: 'demo' <host> <port> [file] \n"
        + "  - host = é o host do barramento\n"
        + "  - port = é a porta do barramento\n"
        + "  - file = arquivo com informações do compartilhamento de autenticação (opicional)";
    // verificando parametros de entrada
    if (args.length < 2) {
      System.out.println(String.format(help, "", ""));
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
      System.out.println(Utils.port);
      System.exit(1);
      return;
    }
    // - arquivo
    String file = "sharedauth.dat";
    if (args.length > 2) {
      file = args[2];
    }

    // inicializando e configurando o ORB
    ORB orb = ORBInitializer.initORB();
    // recuperando o gerente de contexto de chamadas à barramentos 
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    // conectando ao barramento.
    Connection connection = context.createConnection(host, port);
    context.setDefaultConnection(connection);

    // recuperando informações de compartilhamento de autenticação
    LoginProcess process = null;
    byte[] secret = null;
    try {
      FileReader freader = new FileReader(file);
      BufferedReader breader = new BufferedReader(freader);
      try {
        process =
          LoginProcessHelper.narrow(orb.string_to_object(breader.readLine()));
        secret = breader.readLine().getBytes();
        breader.close();
      }
      finally {
        breader.close();
      }
    }
    catch (IOException e) {
      System.err.println("Erro ao ler recuperar dados de arquivo.");
      e.printStackTrace();
      System.exit(1);
    }

    ServiceOfferDesc[] services;
    try {
      // autentica-se no barramento
      connection.loginBySharedAuth(process, secret);
      // busca por serviço
      ServiceProperty[] properties = new ServiceProperty[1];
      properties[0] = new ServiceProperty("offer.domain", "Demo Hello");
      services = context.getOfferRegistry().findServices(properties);
    }
    // login by password
    catch (AccessDenied e) {
      System.err
        .println("segredo fornecido não corresponde ao esperado pelo barramento");
      System.exit(1);
      return;
    }
    catch (InvalidLoginProcess e) {
      System.err.println("processo de login inválido");
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
        System.err.println("não há um login válido no momento");
      }
      System.exit(1);
      return;
    }

    // analiza as ofertas encontradas
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
            System.err.println("não há um login de '%s' válido no momento");
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
