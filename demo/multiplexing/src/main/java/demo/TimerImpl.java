package demo;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.TRANSIENT;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;

public class TimerImpl extends TimerPOA {

  private OpenBusContext context;

  public TimerImpl(OpenBusContext context) {
    this.context = context;
  }

  @Override
  public void newTrigger(double timeout, final Callback cb) {
    final long time = (long) timeout;
    final CallerChain chain = context.getCallerChain();
    final Connection conn = context.getCurrentConnection();
    final String entity = conn.login().entity;
    new Thread() {
      @Override
      public void run() {
        try {
          Thread.sleep(time * 1000);
        }
        catch (InterruptedException e) {
          // do nothing
        }
        context.setCurrentConnection(conn);
        context.joinChain(chain);
        try {
          System.out.println(String.format("timer %s notifying with login %s",
            this.hashCode(), conn.login().id));
          cb.notifyTrigger();
        }
        // Servi�o
        catch (TRANSIENT e) {
          System.err.println("o servi�o encontrado encontra-se indispon�vel");
        }
        catch (COMM_FAILURE e) {
          System.err.println("falha de comunica��o na notifica��o da callback");
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
    }.start();
  }

}
