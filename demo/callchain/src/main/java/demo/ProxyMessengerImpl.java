package demo;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.Object;
import org.omg.CORBA.TRANSIENT;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.demo.util.Utils;

public class ProxyMessengerImpl extends MessengerPOA {

  private OpenBusContext context;
  private String entity;
  private ServiceOfferDesc[] offers;

  public ProxyMessengerImpl(OpenBusContext context, String entity) {
    this.context = context;
    this.entity = entity;
  }

  @Override
  public void showMessage(String message) throws Unavailable, Unauthorized {
    CallerChain chain = context.getCallerChain();
    System.out.println(String.format("repassando mensagem de %s", Utils
      .chain2str(chain)));
    context.joinChain(chain);
    for (ServiceOfferDesc offer : this.offers) {
      boolean failed = true;
      try {
        Object facet = offer.service_ref.getFacet(MessengerHelper.id());
        Messenger messenger = MessengerHelper.narrow(facet);
        messenger.showMessage(message);
        failed = false;
      }
      // Service
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
      finally {
        if (!failed) {
          return;
        }
      }
    }
    System.err.println("servi�os encontrados n�o est�o dispon�veis");
    throw new Unavailable();
  }

  public void setOffers(ServiceOfferDesc[] offers) {
    this.offers = offers;
  }
}
