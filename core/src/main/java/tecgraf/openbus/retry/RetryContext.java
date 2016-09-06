package tecgraf.openbus.retry;

import org.omg.CORBA.NO_PERMISSION;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;

import java.util.concurrent.TimeUnit;

/**
 * Classe que define a política a ser adotada sobre tarefas
 * repassadas a um {@link RetryTaskPool}.
 *
 * @author Tecgraf
 */
public class RetryContext {

  /** Contador de retentativas */
  private volatile int count = 0;
  /** Tempo de intervalo entre tentativas */
  private final long delay;
  /** Unidade do tempo de intervalo entre tentativas */
  private final TimeUnit delayUnit;
  /** Última exceção ocorrida entre as tentativas */
  private volatile Exception lastException = null;

  /**
   * Construtor padrão que aguarda 1 segundo entre retentativas.
   */
  public RetryContext() {
    delay = 1;
    delayUnit = TimeUnit.SECONDS;
  }

  /**
   * Construtor.
   * 
   * @param delay Tempo de espera entre tentativas.
   * @param unit Unidade do tempo entre tentativas.
   */
  public RetryContext(long delay, TimeUnit unit) {
    this.delay = delay;
    this.delayUnit = unit;
  }

  /**
   * Fornece o número de retentativas já realizadas.
   * 
   * @return O número de retentativas.
   */
  public int getRetryCount() {
    return count;
  }

  /**
   * Fornece o tempo a ser utilizado entre retentativas.
   * <p>
   * Este método deve ser sobrescrito caso deseje-se implementar cálculos
   * mais sofisticados de intervalos entre tentativas.
   * 
   * @return O tempo de intervalo a ser aplicado entre as tentativas.
   */
  public long getDelay() {
    return delay;
  }

  /**
   * Fornece a unidade de tempo utilizada para o intervalo.
   *
   * @return A unidade de tempo do intervalo.
   */
  public TimeUnit getDelayUnit() {
    return delayUnit;
  }

  /**
   * Fornece a última exceção ocorrida durante as tentativas.
   * 
   * @return O último erro ocorrido.
   */
  public Exception getLastException() {
    return lastException;
  }

  /**
   * Informa se é permitida uma nova execução da tarefa. Sempre é permitida
   * uma nova execução, a não ser que o erro seja um NO_PERMISSION{NoLogin},
   * que só ocorrerá caso o usuário tenha feito um logout explícito ou não
   * tenha feito login.
   *
   * Este método pode ser sobrescrito caso deseje-se uma lógica diferente.
   *
   * @return {@code True} caso seja permitida uma nova tentativa, e {@code
   *         false} caso contrário.
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
