package tecgraf.openbus.retry;

import org.omg.CORBA.NO_PERMISSION;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;

import java.util.concurrent.TimeUnit;

/**
 * Classe que define a pol�tica a ser adotada sobre tarefas
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
  /** �ltima exce��o ocorrida entre as tentativas */
  private volatile Exception lastException = null;

  /**
   * Construtor padr�o que aguarda 1 segundo entre retentativas.
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
   * Fornece o n�mero de retentativas j� realizadas.
   * 
   * @return O n�mero de retentativas.
   */
  public int getRetryCount() {
    return count;
  }

  /**
   * Fornece o tempo a ser utilizado entre retentativas.
   * <p>
   * Este m�todo deve ser sobrescrito caso deseje-se implementar c�lculos
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
   * Fornece a �ltima exce��o ocorrida durante as tentativas.
   * 
   * @return O �ltimo erro ocorrido.
   */
  public Exception getLastException() {
    return lastException;
  }

  /**
   * Informa se � permitida uma nova execu��o da tarefa. Sempre � permitida
   * uma nova execu��o, a n�o ser que o erro seja um NO_PERMISSION{NoLogin},
   * que s� ocorrer� caso o usu�rio tenha feito um logout expl�cito ou n�o
   * tenha feito login.
   *
   * Este m�todo pode ser sobrescrito caso deseje-se uma l�gica diferente.
   *
   * @return {@code True} caso seja permitida uma nova tentativa, e {@code
   *         false} caso contr�rio.
   */
  public boolean shouldRetry() {
    //TODO implementar configura��o do usu�rio sobre n�mero m�ximo de
    // retentativas. Pode-se seguir um approach similar ao da pcall onde se
    // define um n�mero de retentativas diferente para cada caso (inacess�vel
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
