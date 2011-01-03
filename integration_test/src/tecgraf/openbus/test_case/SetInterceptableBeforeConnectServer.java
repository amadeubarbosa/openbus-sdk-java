package tecgraf.openbus.test_case;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.launcher.ServerTestContext;
import testidl.hello.IHelloHelper;

/**
 * <p>
 * Este teste verifica se o m�todo setInterceptable est� funcionando
 * corretamente no servidor. Sendo executado antes do Openbus.connect().
 * </p>
 * <p>
 * Esta classe deve configurar um m�todo para n�o ser interceptado. Neste caso,
 * o m�todo <i>sayHello</i> da interface <i>IHello</i> ser� o escolhido.
 * Consequentemente, o cliente conseguir� executa-lo mesmo n�o possuindo
 * credencial.
 * </p>
 * <p>
 * � esperado que a superclasse utilize a mesma IDL que o
 * <i>DefaultServerTestCase</i>. Caso isso n�o ocorra, � necess�rio
 * reimplementar os m�todos <i>createComponent</i> e <i>registerComponent</i>.
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
