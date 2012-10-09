package demo;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
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
public final class CallChainClient {
  /**
   * Fun��o principal.
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
      System.out.println(String.format(Utils.clientUsage, "", ""));
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
    // - entidade
    String entity = args[2];
    // - senha (opicional)
    String password = entity;
    if (args.length > 3) {
      password = args[3];
    }

    // recuperando o assistente
    final Assistant assist =
      Assistant.createWithPassword(host, port, entity, password.getBytes());

    // busca por servi�o
    ServiceProperty[] props =
      new ServiceProperty[] {
          new ServiceProperty("offer.domain", "Demo Call Chain"),
          new ServiceProperty("openbus.component.interface", MessengerHelper
            .id()) };
    ServiceOfferDesc[] services = assist.findServices(props, -1);

    // analiza as ofertas encontradas
    for (ServiceOfferDesc offerDesc : services) {
      try {
        org.omg.CORBA.Object msgObj =
          offerDesc.service_ref.getFacet(MessengerHelper.id());
        if (msgObj == null) {
          System.out
            .println("o servi�o encontrado n�o prov� a faceta ofertada");
          continue;
        }

        Messenger messenger = MessengerHelper.narrow(msgObj);
        messenger.showMessage("Hello!");
      }
      // Demo
      catch (Unavailable e) {
        System.err.println(String.format(
          "servi�o com papel '%s' esta indispon�vel", Utils.getProperty(
            offerDesc, "offer.role")));
      }
      catch (Unauthorized e) {
        System.err.println(String.format(
          "servi�o com papel '%s' n�o autorizou a chamada", Utils.getProperty(
            offerDesc, "offer.role")));
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
    }

    // Finaliza o assistente
    assist.shutdown();
  }

}