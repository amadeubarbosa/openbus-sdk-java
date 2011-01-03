package tecgraf.openbus.test_case;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.launcher.ServerTestContext;
import testidl.hello.IHelloHelper;

/**
 * <p>
 * Este teste verifica se o método setInterceptable está funcionando
 * corretamente no servidor. Sendo executado antes do Openbus.connect().
 * </p>
 * <p>
 * Esta classe deve configurar um método para não ser interceptado. Neste caso,
 * o método <i>sayHello</i> da interface <i>IHello</i> será o escolhido.
 * Consequentemente, o cliente conseguirá executa-lo mesmo não possuindo
 * credencial.
 * </p>
 * <p>
 * É esperado que a superclasse utilize a mesma IDL que o
 * <i>DefaultServerTestCase</i>. Caso isso não ocorra, é necessário
 * reimplementar os métodos <i>createComponent</i> e <i>registerComponent</i>.
 * </p>
 * 
 * @author Tecgraf
 */
public class SetInterceptableBeforeConnectServer extends DefaultServerTestCase {

  /**
   * {@inheritDoc}
   */
  @Override
  public void connect(ServerTestContext context) throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    openbus.setInterceptable(IHelloHelper.id(), "sayHello", false);

    super.connect(context);
  }
}
