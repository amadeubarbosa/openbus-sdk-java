package tecgraf.openbus.remote.testCase;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.remote.launcher.ServerTestContext;
import testidl.hello.IHelloHelper;

/**
 * <p>
 * Este teste verifica se o método setInterceptable está funcionando
 * corretamente no servidor.
 * </p>
 * <p>
 * Esta classe deve testar se o setInterceptable funciona corretamente quando
 * passamos <code>true</code> no parâmetro <code>boolean interceptable</code>.
 * Como sabemos que, por padrão, todos os método são interceptados, este testes
 * verifica se é possível definir que um método é interceptavél após defini-lo
 * como não interceptável. Neste caso, o método <i>sayHello</i> da interface
 * <i>IHello</i> será o escolhido para o teste.
 * </p>
 * <p>
 * É esperado que a classe utilize a mesma IDL do <i>DefaultServerTestCase</i>.
 * Caso isso não ocorra, é necessário reimplementar os métodos
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
