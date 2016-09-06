package tecgraf.openbus.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Mecanismo de execu��o de tarefas ass�ncronas atrav�s de um <i>pool</i> de
 * <i>threads</i>. Em caso de falha na execu��o de uma tarefa, a mesma poder�
 * ser novamente executada, de acordo com as regras associadas ao seu
 * contexto de execu��o ({@link RetryContext}).
 *
 * As <i>threads</i> ser�o criadas como <i>daemon</i>, ou seja, morrer�o caso
 * n�o haja mais nenhuma <i>thread</> normal ativa.
 *
 * @author Tecgraf
 */
public class RetryTaskPool {

  /** Tamanho padr�o do pool de threads. */
  private static final int DEFAULT_POOL_SIZE = 4;

  /** Executor de tarefas agend�veis */
  private final ListeningScheduledExecutorService pool;

  /**
   * Construtor padr�o que utiliza 4 threads.
   */
  public RetryTaskPool() {
    this(DEFAULT_POOL_SIZE);
  }

  /**
   * Construtor.
   * 
   * @param size N�mero de threads aloc�veis a serem utilizadas pelo {@code
   * pool}.
   */
  public RetryTaskPool(int size) {
    /*
      Cria threads Daemon para serem utilizadas pelo pool
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
   * @param callable Tarefa a ser executada.
   * @param context Contexto associado a esta tarefa.
   * @return Objeto futuro com o resultado desta execu��o.
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
   * @param runnable Tarefa a ser executada.
   * @param context Contexto associado a esta tarefa.
   * @return Objeto futuro com o resultado desta execu��o.
   */
  public ListenableFuture<Void> doTask(Runnable runnable, RetryContext context) {
    RetryTask<Void> retryTask =
      new RetryTask<>(this.pool, runnable, context);
    retryTask.execute();
    return retryTask.getFuture();
  }

  /**
   * Fornece o <i>pool</i> de <i>threads</i> utilizado.
   *
   * @return O <i>pool</i> de <i>threads</i>.
   */
  public ListeningScheduledExecutorService pool() {
    return pool;
  }
}
