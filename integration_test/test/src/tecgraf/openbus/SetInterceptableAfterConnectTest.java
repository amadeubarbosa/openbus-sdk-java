package tecgraf.openbus;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import tecgraf.openbus.launcher.ClientLauncher;
import tecgraf.openbus.launcher.ServerLaucher;
import tecgraf.openbus.test_case.InterceptedMethodTestCase;
import tecgraf.openbus.test_case.SetInterceptableAfterConnectServer;
import tecgraf.openbus.util.JavaProccess;

/**
 * Este teste executa o servidor <i>SetInterceptableAfterConnectServer</i> e o
 * cliente <i>InterceptedMethodTestCase</i>, para verificar se o interceptador
 * servidor está respondendo corretamente o método <i>setInterceptable</i> e
 * <i>isInterceptable</i>.
 * 
 * 
 * @author Tecgraf
 */
public class SetInterceptableAfterConnectTest {

  /**
   * Processo que representa o teste servidor.
   */
  private static JavaProccess proccess;

  /**
   * Tempo para que o servidor se termine sua inicialização.
   */
  private static final int SLEEP_TIME = 5000;

  /**
   * Método chamado antes de todos os testCases.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  @BeforeClass
  public static void startTestSuite() throws IOException, InterruptedException {
    proccess = new JavaProccess(ServerLaucher.class, "Server");
    String[] args =
      new String[] { SetInterceptableAfterConnectServer.class
        .getCanonicalName() };
    proccess.setArgs(args);
    proccess.exec();
    proccess.redirectOut(System.out);
    proccess.redirectErr(System.err);
    Thread.sleep(SLEEP_TIME);
  }

  /**
   * Método chamado depois de todos os testCases.
   * 
   * @throws InterruptedException
   */
  @AfterClass
  public static void stopTestSuit() throws InterruptedException {
    if (proccess != null && proccess.isRunning())
      proccess.kill();
  }

  /**
   * Testa se o método que não está sendo interceptado pelo servidor, responde
   * corretamente (não lança a exceção NO_PERMISSION).
   * 
   * @throws IOException
   */
  @Test
  public void setInterceptableAfterConnectServer() throws IOException {
    String testCaseName = InterceptedMethodTestCase.class.getCanonicalName();
    Assert.assertEquals(ClientLauncher.exec(testCaseName), 0);
  }

}
