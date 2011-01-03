package tecgraf.openbus.test_case;

import tecgraf.openbus.launcher.ClientTestContext;
import testidl.hello.IHello;
import testidl.hello.IHelloHelper;

/**
 * <p>
 * Este teste verifica se o m�todo setInterceptable est� funcionando
 * corretamente no servidor.
 * </p>
 * <p>
 * O servidor deve configurar um m�todo para n�o ser interceptado. Isso quer
 * dizer que o cliente conseguir� executa-lo mesmo n�o possuindo credencial.
 * </p>
 * 
 * @author Tecgraf
 */
public class InterceptedMethodTestCase extends DefaultClientTestCase {

  /**
   * {@inheritDoc}
   * 
   * Executa uma chamada <i>sayHello</i> ap�s se desconectar do barramento.
   */
  @Override
  public void disconnect(ClientTestContext context) {
    super.disconnect(context);

    org.omg.CORBA.Object servant = context.servant;
    IHello hello = IHelloHelper.narrow(servant);

    hello.sayHello();
  }
}
