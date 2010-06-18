package tecgraf.openbus.remote.testCase;

import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.UserException;

import scs.core.IComponent;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.remote.launcher.ClientTestContext;
import tecgraf.openbus.util.Log;
import testidl.hello.IHello;
import testidl.hello.IHelloHelper;

/**
 * <p>
 * Classe responsável por implementar a interface ClientTestCase de forma
 * padrão.
 * </p>
 * <p>
 * Replica o comportamento do demo hello.
 * </p>
 * 
 * @author Tecgraf
 */
public class DefaultClientTestCase implements ClientTestCase {

  /**
   * {@inheritDoc}
   * 
   * @throws UserException Caso ocorra um erro ao iniciar o RootPOA.
   */
  @Override
  public void init(ClientTestContext context) throws OpenBusException,
    UserException {

    Properties props = context.properties;
    String host = props.getProperty("host.name");
    String portString = props.getProperty("host.port");
    int port = Integer.valueOf(portString);

    Openbus openbus = Openbus.getInstance();

    Log.setLogsLevel(Level.FINEST);
    Properties orbProps = new Properties();
    orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");

    openbus.initWithFaultTolerance(null, orbProps, host, port);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void connect(ClientTestContext context) throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    Properties props = context.properties;
    String userLogin = props.getProperty("user.login");
    String userPassword = props.getProperty("user.password");

    openbus.connect(userLogin, userPassword);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void findOffer(ClientTestContext context) {
    Openbus openbus = Openbus.getInstance();
    IRegistryService registryService = openbus.getRegistryService();
    ServiceOffer[] servicesOffers =
      registryService.find(new String[] { IHelloHelper.id() });
    ServiceOffer serviceOffer = servicesOffers[0];

    IComponent component = serviceOffer.member;
    org.omg.CORBA.Object helloObject = component.getFacet(IHelloHelper.id());
    context.servant = IHelloHelper.narrow(helloObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void executeServant(ClientTestContext context) {
    org.omg.CORBA.Object servant = context.servant;
    IHello hello = IHelloHelper.narrow(servant);

    hello.sayHello();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect(ClientTestContext context) {
    Openbus openbus = Openbus.getInstance();
    openbus.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy(ClientTestContext context) {
    Openbus openbus = Openbus.getInstance();
    openbus.destroy();
  }
}
