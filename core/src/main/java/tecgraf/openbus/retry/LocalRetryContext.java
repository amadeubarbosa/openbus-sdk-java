package tecgraf.openbus.retry;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.SystemException;

import java.util.concurrent.TimeUnit;

public class LocalRetryContext extends RetryContext {

  /**
   * Construtor
   */
  public LocalRetryContext() {
    super();
  }

  /**
   * Construtor
   *
   * @param delay tempo de espera entre tentativas
   */
  public LocalRetryContext(long delay, TimeUnit unit) {
    super(delay, unit);
  }

  /**
   * Informa se � permitida uma nova execu��o da tarefa.
   * Sempre � permitida uma nova execu��o, a n�o ser que o objeto
   * n�o exista mais no servidor ou o �ltimo erro seja um COMM_FAILURE ou uma
   * exce��o da aplica��o.
   *
   * @return <code>true</code> caso seja permitido uma nova tentativa, e <code>
   *         false</code> caso contr�rio.
   */
  @Override
  public boolean shouldRetry() {
    //TODO implementar configura��o do usu�rio sobre n�mero m�ximo de
    // retentativas. Pode-se seguir um approach similar ao da pcall onde se
    // define um n�mero de retentativas diferente para cada caso (inacess�vel
    // ou quebrado).
    Exception last = getLastException();
    return last instanceof SystemException && !(last instanceof
      OBJECT_NOT_EXIST) && !(last instanceof COMM_FAILURE);
  }
}
