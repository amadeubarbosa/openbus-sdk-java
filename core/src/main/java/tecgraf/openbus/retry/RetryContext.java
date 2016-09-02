package tecgraf.openbus.retry;

import org.omg.CORBA.NO_PERMISSION;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;

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
   * Informa se � permitida uma nova execu��o da tarefa.
   * Sempre � permitida uma nova execu��o, a n�o ser que o erro seja um
   * NO_PERMISSION{NoLogin}, que s� ocorrer� caso o usu�rio tenha feito um
   * logout expl�cito ou n�o tenha feito login.
   *
   * @return <code>true</code> caso seja permitido uma nova tentativa, e <code>
   *         false</code> caso contr�rio.
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
