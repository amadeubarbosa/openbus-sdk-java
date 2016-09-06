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
   * Informa se é permitida uma nova execução da tarefa.
   * Sempre é permitida uma nova execução, a não ser que o objeto
   * não exista mais no servidor, o erro seja um COMM_FAILURE, uma exceção da
   * aplicação ou um NO_PERMISSION{NoLogin}, que só ocorrerá caso o usuário
   * tenha feito um logout explícito ou não tenha feito login.
   *
   * @return {@code true} caso seja permitido uma nova tentativa, e {@code
   *         false} caso contrário.
   */
  @Override
  public boolean shouldRetry() {
    Exception last = getLastException();
    boolean noLogin = super.shouldRetry();
    return !noLogin && last instanceof SystemException && !(last instanceof
      OBJECT_NOT_EXIST) && !(last instanceof COMM_FAILURE);
  }
}
