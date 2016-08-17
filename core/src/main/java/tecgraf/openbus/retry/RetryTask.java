package tecgraf.openbus.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Classe que permite retentativas de execu��o em caso de falhas.
 *
 * @author Tecgraf
 * @param <T> Tipo de retorno esperado
 */
class RetryTask<T> {

  /** Inst�ncia do logger */
  private static final Logger logger = Logger.getLogger(RetryTask.class
    .getName());

  /** Servi�o de execu��o de tarefas. */
  private ListeningScheduledExecutorService pool;
  /** A tarefa a ser executada */
  private Callable<T> task;
  /** Objeto futuro associado a esta tarefa */
  private SettableFuture<T> future;
  /** Contexto de execu��o desta tarefa */
  private RetryContext context;
  /** Callback de p�s execu��o de uma tarefa */
  private FutureCallback<T> callback;

  /**
   * Construtor
   * 
   * @param scheduler executor de tarefas.
   * @param callable tarefa a ser executada.
   */
  public RetryTask(ListeningScheduledExecutorService scheduler,
    Callable<T> callable) {
    this(scheduler, callable, new RetryContext());
  }

  /**
   * Construtor
   * 
   * @param scheduler executor de tarefas.
   * @param callable tarefa a ser executada.
   * @param context contexto associado a tarefa a ser executada.
   */
  public RetryTask(ListeningScheduledExecutorService scheduler,
    Callable<T> callable, RetryContext context) {
    this.future = SettableFuture.create();
    this.pool = scheduler;
    this.task = callable;
    this.context = context;
    this.callback = new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        complete(result);
      }

      @Override
      public void onFailure(Throwable t) {
        handleException(t);
      }
    };
  }

  /**
   * Construtor
   * 
   * @param scheduler executor de tarefas.
   * @param runnable tarefa a ser executada.
   */
  public RetryTask(ListeningScheduledExecutorService scheduler,
    Runnable runnable) {
    this(scheduler, runnable, new RetryContext());
  }

  /**
   * Construtor
   * 
   * @param scheduler executor de tarefas.
   * @param runnable tarefa a ser executada.
   * @param context contexto associado a tarefa a ser executada.
   */
  public RetryTask(ListeningScheduledExecutorService scheduler,
    final Runnable runnable, RetryContext context) {
    this(scheduler, () -> {
      runnable.run();
      return null;
    }, context);
  }

  /**
   * M�todo respons�vel por iniciar a execu��o da tarefa com retentativas em
   * uma nova thread.
   */
  public void execute() {
    ListenableFuture<T> result = pool.submit(task);
    // aqui n�o preciso me preocupar com o resultado de setFuture pois a
    // aplica��o n�o o recebeu ainda e RetryTaskPool n�o o cancela nem seta
    // antes que este m�todo acabe.
    future.setFuture(result);
    Futures.addCallback(result, callback, pool);
  }

  /**
   * Tratamento em caso de falhas.
   * 
   * @param t exce��o ocorrida na �ltima execu��o.
   */
  protected void handleException(Throwable t) {
    context.setLastException((Exception) t);
    if (!future.isCancelled()) {
      if (context.shouldRetry()) {
        context.incrementRetrys();
        retryWithDelay();
      }
      else {
        logger.finest(String.format(
          "Failure! Max retries reached! Last exception: %s", t));
        future.setException(t);
      }
    }
  }

  /**
   * Dispara uma nova tentativa respeitando o tempo de intervalo entre
   * elas.
   */
  private void retryWithDelay() {
    long delay = context.getDelay();
    TimeUnit unit = context.getDelayUnit();
    Exception ex = context.getLastException();
    ListenableScheduledFuture<T> submit = pool.schedule(task, delay, unit);
    if (!future.setFuture(submit)) {
      // ja terminou ou foi cancelado, nao deveria retentar.
      // se foi cancelado, submit automaticamente foi cancelado em setFuture.
      // se future ja terminou, devo cancelar submit e pode ser bug de
      // concorr�ncia
      if (future.isDone()) {
        //TODO quando chega aqui n�o est� cancelando corretamente!
        submit.cancel(false);
        logger.severe("The task had already completed but a retry was being " +
          "attempted. Check for concurrency problems.");
      }
    } else {
      Futures.addCallback(submit, callback, pool);
      logger.finest(String
        .format("Task failed! Scheduled next retry in %d time units. The time unit is %s. Last exception: %s",
          delay, unit, ex));
    }
  }

  /**
   * Tratamento em caso de sucesso.
   * 
   * @param result o resultado da execu��o da tarefa.
   */
  protected void complete(T result) {
    logger.finest("Success on execution!");
  }

  /**
   * Recupera o objeto futuro associado a esta tarefa.
   * 
   * @return o objeto futuro.
   */
  public ListenableFuture<T> getFuture() {
    return future;
  }

}
