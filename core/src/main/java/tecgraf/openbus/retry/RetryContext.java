package tecgraf.openbus.retry;

import org.omg.CORBA.NO_PERMISSION;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;

import java.util.concurrent.TimeUnit;

/**
 * Classe que define política a ser adotada sobre {@link RetryTask}.
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
  /** Última exceção ocorrida entre as tentativas */
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
   * Recupera o número de retentativas já realizadas.
   * 
   * @return o número de retentativas.
   */
  public int getRetryCount() {
    return count;
  }

  /**
   * O padrão é um intervalo fixo.
   * <p>
   * Este método deve ser sobrescrito caso queira implementar cálculos de
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
   * Recupera a última exceção ocorrida durante as tentativas.
   * 
   * @return o último erro ocorrido.
   */
  public Exception getLastException() {
    return lastException;
  }

  /**
   * Informa se é permitida uma nova execução da tarefa.
   * Sempre é permitida uma nova execução, a não ser que o erro seja um
   * NO_PERMISSION{NoLogin}, que só ocorrerá caso o usuário tenha feito um
   * logout explícito ou não tenha feito login.
   *
   * @return <code>true</code> caso seja permitido uma nova tentativa, e <code>
   *         false</code> caso contrário.
   */
  public boolean shouldRetry() {
    //TODO implementar configuração do usuário sobre número máximo de
    // retentativas. Pode-se seguir um approach similar ao da pcall onde se
    // define um número de retentativas diferente para cada caso (inacessível
    // ou quebrado).
    Exception last = getLastException();
    if (last instanceof NO_PERMISSION) {
      if (((NO_PERMISSION) last).minor == NoLoginCode.value) {
        return false;
      }
    }
    return true;
  }

  /**
   * Incrementa o número de tentativas.
   */
  protected void incrementRetrys() {
    this.count++;
  }

  /**
   * Define a última exceção ocorrida.
   * 
   * @param ex a exceção.
   */
  protected void setLastException(Exception ex) {
    this.lastException = ex;
  }
}
