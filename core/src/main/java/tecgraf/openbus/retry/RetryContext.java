package tecgraf.openbus.retry;

import org.omg.CORBA.OBJECT_NOT_EXIST;

import java.util.concurrent.TimeUnit;

/**
 * Classe que define pol�tica a ser adotada sobre {@link RetryTask}.
 *
 * @author Tecgraf
 */
public class RetryContext {

  /** Contador de retentativas */
  private int count = 0;
  /** Tempo de intervalo entre tentativas */
  private long delay = 1;
  /** Unidade do tempo de intervalo entre tentativas */
  private TimeUnit delayUnit = TimeUnit.SECONDS;
  /** �ltima exce��o ocorrida entre as tentativas */
  private Exception lastException = null;

  /**
   * Construtor
   */
  public RetryContext() {
  }

  /**
   * Construtor
   * 
   * @param delay tempo de espera entre tentativas
   */
  public RetryContext(long delay, TimeUnit unit) {
    this.delay = delay;
    this.delayUnit = unit;
  }

  /**
   * Recupera o n�mero de retentativas j� realizadas.
   * 
   * @return o n�mero de retentativas.
   */
  public int getRetryCount() {
    return count;
  }

  /**
   * O padr�o � um intervalo fixo.
   * <p>
   * Este m�todo deve ser sobrescrito caso queira implementar c�lculos de
   * intervalos entre tentativas mais sofisticados.
   * 
   * @return o tempo de intervalo a ser aplicado entre as tentativas.
   */
  public long getDelay() {
    return delay;
  }

  /**
   * Recupera a unidade de tempo utilizada para o intervalo.
   *
   * @return a unidade de tempo do intervalo.
   */
  public TimeUnit getDelayUnit() {
    return delayUnit;
  }

  /**
   * Recupera a �ltima exce��o ocorrida durante as tentativas.
   * 
   * @return o �ltimo erro ocorrido.
   */
  public Exception getLastException() {
    return lastException;
  }

  /**
   * Informa se � permitido uma nova execu��o da tarefa.
   * <p>
   * Por padr�o sempre � permitida uma nova execu��o, a n�o ser que o objeto
   * n�o exista mais no servidor. Este m�todo deve ser sobreescrito caso
   * deseje-se implementar algum tipo de limitador. Por exemplo:
   * <li>n�mero m�ximo de tentativas
   * <li>�ltima exce��o indica um erro irrecuper�vel.
   * </p>
   *
   * @return <code>true</code> caso seja permitido uma nova tentativa, e <code>
   *         false</code> caso contr�rio.
   */
  public boolean shouldRetry() {
    return !(getLastException() instanceof OBJECT_NOT_EXIST);
  }

  /**
   * Incrementa o n�mero de tentativas.
   */
  protected void incrementRetrys() {
    this.count++;
  }

  /**
   * Define a �ltima exce��o ocorrida.
   * 
   * @param ex a exce��o.
   */
  protected void setLastException(Exception ex) {
    this.lastException = ex;
  }

}
