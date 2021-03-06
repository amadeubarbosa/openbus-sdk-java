package demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.AuthArgs;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidEncodedStream;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.WrongBus;

/**
 * Demo Single Sign On.
 * 
 * @author Tecgraf
 */
public class SharedAuthClient {

  /**
   * Fun��o main.
   * 
   */
  public static void main(String[] args) throws ServiceFailure, InvalidName,
    AlreadyLoggedIn, TooManyAttempts, UnknownDomain, MissingCertificate {
    String help =
      "Usage: 'demo' <host> <port> [file] \n"
        + "  - host = � o host do barramento\n"
        + "  - port = � a porta do barramento\n"
        + "  - file = arquivo com informa��es do compartilhamento de autentica��o (opcional)";
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
      System.out.println(Usage.port);
      System.exit(1);
      return;
    }
    // - arquivo
    String temp = "sharedauth.dat";
    if (args.length > 2) {
      temp = args[2];
    }
    final String path = temp;

    // inicializando e configurando o ORB
    ORB orb = ORBInitializer.initORB();
    // recuperando o gerente de contexto de chamadas a barramentos 
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    // conectando ao barramento.
    Connection connection = context.connectByAddress(host, port);
    context.defaultConnection(connection);

    List<RemoteOffer> services;
    try {
      // autentica-se no barramento
      connection.loginByCallback(() -> {
      // recuperando informa��es de compartilhamento de autentica��o
      /*
       * OBS: talvez seja mais interessante para a aplica��o trocar esses dados de
       * outra forma. No m�nimo, essas informa��es deveriam estar encriptadas.
       * No entanto, isso foge ao escopo dessa demo.
       */
        byte[] data;
        try {
          File file = new File(path);
          try (FileInputStream is = new FileInputStream(file)) {
            int length = (int) file.length();
            data = new byte[length];
            int offset = is.read(data);
            while (offset < length) {
              int read = is.read(data, offset, length - offset);
              if (read < 0) {
                System.err.println("N�o foi poss�vel ler todo o arquivo");
                System.exit(1);
              }
              offset += read;
            }
          }
          return new AuthArgs(context.decodeSharedAuth(data));
        } catch (IOException e) {
          System.err.println("Erro ao recuperar dados do arquivo.");
          e.printStackTrace();
          System.exit(1);
        } catch (InvalidEncodedStream e) {
          System.err
            .println("erro ao decodificar compartilhamento de autentica��o");
          System.exit(1);
        }
        return null;
      });
      // busca por servi�o
      ArrayListMultimap<String, String> properties = ArrayListMultimap
        .create();
      properties.put("offer.domain", "Demo Hello");
      services = connection.offerRegistry().findServices(properties);
    }
    // login by password
    catch (AccessDenied e) {
      System.err
        .println("segredo fornecido n�o corresponde ao esperado pelo barramento");
      System.exit(1);
      return;
    }
    catch (InvalidLoginProcess e) {
      System.err.println("processo de login inv�lido");
      System.exit(1);
      return;
    }
    catch (WrongBus e) {
      System.err.println("tentativa de uso de segredo de outro barramento");
      System.exit(1);
      return;
    }
    catch (WrongEncoding e) {
      System.err
        .println("incompatibilidade na codifi��o de informa��o para o barramento");
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
        System.err.println("n�o h� um login v�lido no momento");
      }
      System.exit(1);
      return;
    }

    // analisa as ofertas encontradas
    for (RemoteOffer offer : services) {
      try {
        org.omg.CORBA.Object helloObj =
          offer.service().getFacet(HelloHelper.id());
        if (helloObj == null) {
          System.out
            .println("o servi�o encontrado n�o prov� a faceta ofertada");
          continue;
        }

        Hello hello = HelloHelper.narrow(helloObj);
        hello.sayHello();
      }
      catch (TRANSIENT e) {
        System.err.println("o servi�o encontrado encontra-se indispon�vel");
      }
      catch (COMM_FAILURE e) {
        System.err.println("falha de comunica��o com o servi�o encontrado");
      }
      catch (NO_PERMISSION e) {
        switch (e.minor) {
          case NoLoginCode.value:
            System.err.println("n�o h� um login de '%s' v�lido no momento");
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
    }

    // Faz o logout
    context.currentConnection().logout();
  }
}
