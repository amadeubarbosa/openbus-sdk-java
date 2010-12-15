package tecgraf.openbus.remote.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.omg.CORBA.UserException;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_06.registry_service.IRegistryService;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.remote.testCase.ClientTestCase;

/**
 * Respons�vel por lan�ar um teste cliente.
 * 
 * @author Tecgraf
 */
public class ClientLauncher {

  /**
   * <p>
   * Executa o teste cliente <i>testCaseName</i>.
   * </p>
   * <p>
   * O m�todo garante que o ClientTestCase.destroy() ser� chamado mesmo que
   * ocorra uma exe��o nos m�todos anteriores.
   * </p>
   * 
   * @param testCaseName O nome da classe que implementa <i>ClientTestCase</i>.
   * @return Retorna 0 caso n�o ocorra erro.
   * @throws IOException
   * 
   */
  public static int exec(String testCaseName) throws IOException {

    Object instance = null;
    try {
      instance = Class.forName(testCaseName).newInstance();
    }
    catch (Exception e) {
      System.err.println("Erro ao tentar instanciar a classe " + testCaseName);
      return -1;
    }

    if (!(instance instanceof ClientTestCase)) {
      System.err.println(String.format("Classe %s n�o � do tipo %s",
        testCaseName, ClientTestCase.class.getCanonicalName()));
      return -1;
    }

    ClientTestCase clientTestCase = (ClientTestCase) instance;
    Openbus openbus = Openbus.getInstance();
    ClientTestContext context = new ClientTestContext();

    Properties props = new Properties();
    InputStream in =
      ClientLauncher.class.getResourceAsStream("/AllTests.properties");
    try {
      props.load(in);
    }
    finally {
      in.close();
    }
    context.properties = props;

    try {
      clientTestCase.init(context);

      clientTestCase.connect(context);

      IRegistryService registryService = openbus.getRegistryService();
      if (registryService == null) {
        throw new RSUnavailableException();
      }

      clientTestCase.findOffer(context);

      clientTestCase.executeServant(context);

      clientTestCase.disconnect(context);
    }
    catch (OpenBusException e) {
      e.printStackTrace();
      return -1;
    }
    catch (UserException e) {
      e.printStackTrace();
      return -1;
    }
    catch (Exception e) {
      return -1;
    }
    finally {
      clientTestCase.destroy(context);
    }

    return 0;
  }
}
