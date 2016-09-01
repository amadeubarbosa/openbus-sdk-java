package tecgraf.openbus.core;

import java.util.concurrent.TimeUnit;

class InfiniteRetryContext extends OpenBusRetryContext {
  /**
   * Construtor
   *
   * @param delay tempo de espera entre tentativas
   */
  public InfiniteRetryContext(long delay, TimeUnit unit) {
    super(delay, unit);
  }

  /**
   * Informa se é permitida uma nova execução da tarefa.
   * Sempre é permitida uma nova execução, pois o usuário pode consultar o
   * erro mais recente através da LocalOffer e cancelar se desejar.
   *
   * @return <code>true</code> caso seja permitido uma nova tentativa, e <code>
   *         false</code> caso contrário.
   */
  @Override
  public boolean shouldRetry() {
    return true;
  }
}
