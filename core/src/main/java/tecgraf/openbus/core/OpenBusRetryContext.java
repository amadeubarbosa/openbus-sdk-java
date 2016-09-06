package tecgraf.openbus.core;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.SystemException;
import tecgraf.openbus.retry.RetryContext;

import java.util.concurrent.TimeUnit;

class OpenBusRetryContext extends RetryContext {
  /**
   * Construtor
   *
   * @param delay tempo de espera entre tentativas
   */
  public OpenBusRetryContext(long delay, TimeUnit unit) {
    super(delay, unit);
  }

  /**
   * Informa se � permitida uma nova execu��o da tarefa.
   * Sempre � permitida uma nova execu��o, a n�o ser que o objeto
   * n�o exista mais no servidor, o erro seja um COMM_FAILURE, uma exce��o da
   * aplica��o ou um NO_PERMISSION{NoLogin}, que s� ocorrer� caso o usu�rio
   * tenha feito um logout expl�cito ou n�o tenha feito login.
   *
   * @return {@code true} caso seja permitido uma nova tentativa, e {@code
   *         false} caso contr�rio.
   */
  @Override
  public boolean shouldRetry() {
    Exception last = getLastException();
    boolean noLogin = super.shouldRetry();
    return !noLogin && last instanceof SystemException && !(last instanceof
      OBJECT_NOT_EXIST) && !(last instanceof COMM_FAILURE);
  }
}
