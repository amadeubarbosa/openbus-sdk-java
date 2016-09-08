package tecgraf.openbus.retry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Classe que permite retentativas de execução em caso de falhas.
 *
 * @author Tecgraf
 * @param <T> Tipo de retorno esperado
 */
class RetryTask<T> {

  /** Instância do logger */
  private static final Logger logger = Logger.getLogger(RetryTask.class
    .getName());

  /** Serviço de execução de tarefas. */
  private final ListeningScheduledExecutorService pool;
  /** A tarefa a ser executada */
  private final Callable<T> task;
  /** Objeto futuro associado a esta tarefa e retornado ao usuário. Só é
   * setado quando result termina, para que possam ser feitas retentativas */
  private final SettableFuture<T> future;
  /** Objeto futuro que realiza o trabalho em si */
  private ListenableFuture<T> result;
  /** Contexto de execução desta tarefa */
  private final RetryContext context;
  /** Callback de pós execução de uma tarefa */
  private final FutureCallback<T> callback;

  /**
   * Construtor
   *
   * @param scheduler O thread pool responsável por executar as tarefas.
   * @param callable A tarefa a ser executada.
   */
  public RetryTask(ListeningScheduledExecutorService scheduler,
    Callable<T> callable) {
    this(scheduler, callable, new RetryContext());
  }

  /**
   * Construtor
   * 
   * @param scheduler O thread pool responsável por executar as tarefas.
   * @param callable A tarefa a ser executada.
   * @param context O contexto de configuração da tarefa.
   */
  public RetryTask(ListeningScheduledExecutorService scheduler,
    Callable<T> callable, RetryContext context) {
    this.future = SettableFuture.create();
    this.result = null;
    this.pool = scheduler;
    this.task = callable;
    this.context = context;
    this.context.future(this.future);
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
   * @param scheduler O thread pool responsável por executar as tarefas.
   * @param runnable A tarefa a ser executada.
   */
  public RetryTask(ListeningScheduledExecutorService scheduler,
    Runnable runnable) {
    this(scheduler, runnable, new RetryContext());
  }

  /**
   * Construtor
   *
   * @param scheduler O thread pool responsável por executar as tarefas.
   * @param runnable A tarefa a ser executada.
   * @param context O contexto de configuração da tarefa.
   */
  public RetryTask(ListeningScheduledExecutorService scheduler,
    final Runnable runnable, RetryContext context) {
    this(scheduler, () -> {
      runnable.run();
      return null;
    }, context);
  }

  /**
   * Método responsável por iniciar a execução da tarefa com retentativas em
   * uma nova thread.
   */
  public void execute() {
    result = pool.submit(task);
    Futures.addCallback(result, callback, pool);
  }

  /**
   * Tratamento em caso de falhas.
   * 
   * @param t exceção ocorrida na última execução.
   */
  protected void handleException(Throwable t) {
    context.setLastException(t);
    if (!future.isCancelled()) {
      if (context.shouldRetry()) {
        context.incrementRetrys();
        retryWithDelay();
      }
      else {
        logger.finest(String.format(
          "Máximo de tentativas alcançado. Última exceção: %s", t));
        future.setFuture(result);
      }
    } else {
      logger.finest("Execução cancelada pelo usuário, não vai fazer retry");
    }
  }

  /**
   * Dispara uma nova tentativa respeitando o tempo de intervalo entre
   * elas.
   */
  private void retryWithDelay() {
    long delay = context.getDelay();
    TimeUnit unit = context.getDelayUnit();
    Throwable ex = context.getLastException();
    logger.finest(String
      .format("Falha na tarefa. Uma nova tentativa será agendada " +
          "para ocorrer em %d %s. Última exceção: %s", delay, unit, ex));
    result = pool.schedule(task, delay, unit);
    Futures.addCallback(result, callback, pool);
  }

  /**
   * Tratamento em caso de sucesso.
   * 
   * @param result o resultado da execução da tarefa.
   */
  protected void complete(T result) {
    // não preciso checar se foi cancelado pois se tiver sido o método vai
    // retornar false e continuar cancelado.
    future.setFuture(this.result);
    logger.finest("Execução bem-sucedida.");
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
