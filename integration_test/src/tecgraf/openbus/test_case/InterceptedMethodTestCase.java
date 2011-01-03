package tecgraf.openbus.test_case;

import tecgraf.openbus.launcher.ClientTestContext;
import testidl.hello.IHello;
import testidl.hello.IHelloHelper;

/**
 * <p>
 * Este teste verifica se o método setInterceptable está funcionando
 * corretamente no servidor.
 * </p>
 * <p>
 * O servidor deve configurar um método para não ser interceptado. Isso quer
 * dizer que o cliente conseguirá executa-lo mesmo não possuindo credencial.
 * </p>
 * 
 * @author Tecgraf
 */
public class InterceptedMethodTestCase extends DefaultClientTestCase {

  /**
   * {@inheritDoc}
   * 
   * Executa uma chamada <i>sayHello</i> após se desconectar do barramento.
   */
  @Override
  public void disconnect(ClientTestContext context) {
    super.disconnect(context);

    org.omg.CORBA.Object servant = context.servant;
    IHello hello = IHelloHelper.narrow(servant);

    hello.sayHello();
  }
}
