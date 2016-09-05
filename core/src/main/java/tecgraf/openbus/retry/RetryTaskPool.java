package tecgraf.openbus.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Mecanismo de execu��o de tarefas ass�ncronas atrav�s de um pool de threads.
 * Em caso de falha na execu��o de uma tarefa ({@link RetryTask}), a mesma
 * poder� ser novamente executada, de acordo com as regras associadas ao
 * seu contexto de execu��o ({@link RetryContext}).
 *
 * @author Tecgraf
 */
public class RetryTaskPool {

  /** Tamanho padr�o do pool de threads. */
  private static final int DEFAULT_POOL_SIZE = 4;

  /** Executor de tarefas agend�veis */
  private final ListeningScheduledExecutorService pool;

  /**
   * Construtor.
   */
  public RetryTaskPool() {
    this(DEFAULT_POOL_SIZE);
  }

  /**
   * Construtor.
   * 
   * @param size n�mero de threads aloc�veis por este mecanismo.
   */
  public RetryTaskPool(int size) {
    /**
     * Cria threads Daemon para serem utilizadas pelo pool
     */
    this.pool =
      MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(size,
        task -> {
          Thread thread = new Thread(task);
          thread.setDaemon(true);
          return thread;
        }));
  }

  /**
   * Dispara uma tarefa que pode ser retentada em caso de falhas, de acordo com
   * as regras definidas no contexto.
   * 
   * @param callable tarefa a ser executada
   * @param context contexto associado a esta tarefa
   * @return objeto futuro com o resultado desta execu��o.
   */
  public <T> ListenableFuture<T> doTask(Callable<T> callable,
    RetryContext context) {
    RetryTask<T> retryTask = new RetryTask<>(this.pool, callable, context);
    retryTask.execute();
    return retryTask.getFuture();
  }

  /**
   * Dispara uma tarefa que pode ser retentada em caso de falhas, de acordo com
   * as regras definidas no contexto.
   * 
   * @param runnable tarefa a ser executada
   * @param context contexto associado a esta tarefa
   * @return objeto futuro com o resultado desta execu��o.
   */
  public ListenableFuture<Void> doTask(Runnable runnable, RetryContext context) {
    RetryTask<Void> retryTask =
      new RetryTask<>(this.pool, runnable, context);
    retryTask.execute();
    return retryTask.getFuture();
  }

  public ListeningScheduledExecutorService pool() {
    return pool;
  }
}
