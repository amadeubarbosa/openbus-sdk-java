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
   * Informa se � permitida uma nova execu��o da tarefa.
   * Sempre � permitida uma nova execu��o, pois o usu�rio pode consultar o
   * erro mais recente atrav�s da LocalOffer e cancelar se desejar.
   *
   * @return <code>true</code> caso seja permitido uma nova tentativa, e <code>
   *         false</code> caso contr�rio.
   */
  @Override
  public boolean shouldRetry() {
    return true;
  }
}
