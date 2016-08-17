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
   * Informa se é permitida uma nova execução da tarefa.
   * Sempre é permitida uma nova execução, a não ser que o objeto
   * não exista mais no servidor ou o último erro seja um COMM_FAILURE ou uma
   * exceção da aplicação.
   *
   * @return <code>true</code> caso seja permitido uma nova tentativa, e <code>
   *         false</code> caso contrário.
   */
  @Override
  public boolean shouldRetry() {
    //TODO implementar configuração do usuário sobre número máximo de
    // retentativas. Pode-se seguir um approach similar ao da pcall onde se
    // define um número de retentativas diferente para cada caso (inacessível
    // ou quebrado).
    Exception last = getLastException();
    return last instanceof SystemException && !(last instanceof
      OBJECT_NOT_EXIST) && !(last instanceof COMM_FAILURE);
  }
}
