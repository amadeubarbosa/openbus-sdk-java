package tecgraf.openbus.remote.testCase;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.remote.launcher.ServerTestContext;
import testidl.hello.IHelloHelper;

/**
 * <p>
 * Este teste verifica se o m�todo setInterceptable est� funcionando
 * corretamente no servidor.
 * </p>
 * <p>
 * Esta classe deve testar se o setInterceptable funciona corretamente quando
 * passamos <code>true</code> no par�metro <code>boolean interceptable</code>.
 * Como sabemos que, por padr�o, todos os m�todo s�o interceptados, este testes
 * verifica se � poss�vel definir que um m�todo � interceptav�l ap�s defini-lo
 * como n�o intercept�vel. Neste caso, o m�todo <i>sayHello</i> da interface
 * <i>IHello</i> ser� o escolhido para o teste.
 * </p>
 * <p>
 * � esperado que a classe utilize a mesma IDL do <i>DefaultServerTestCase</i>.
 * Caso isso n�o ocorra, � necess�rio reimplementar os m�todos
 * <i>createComponent</i> e <i>registerComponent</i>.
 * </p>
 * 
 * @author Tecgraf
 */
public class SetInterceptableMethodServer extends DefaultServerTestCase {

  /**
   * {@inheritDoc}
   */
  @Override
  public void connect(ServerTestContext context) throws OpenBusException {
    super.connect(context);

    Openbus openbus = Openbus.getInstance();
    openbus.setInterceptable(IHelloHelper.id(), "sayHello", false);
    openbus.setInterceptable(IHelloHelper.id(), "sayHello", true);
  }
}
