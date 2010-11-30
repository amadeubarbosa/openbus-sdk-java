package tecgraf.openbus.remote.testCase;

import java.util.Properties;

import org.omg.CORBA.UserException;

import scs.core.IComponent;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.remote.launcher.ClientTestContext;
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
  public void init(ClientTestContext context) throws OpenBusException,
    UserException {

    Properties props = context.properties;
    String host = props.getProperty("Host.Name");
    String portString = props.getProperty("Host.Port");
    int port = Integer.valueOf(portString);

    Openbus openbus = Openbus.getInstance();

    Properties orbProps = new Properties();
    orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");

    openbus.initWithFaultTolerance(null, orbProps, host, port);
  }

  /**
   * {@inheritDoc}
   */
  public void connect(ClientTestContext context) throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    Properties props = context.properties;
    String userLogin = props.getProperty("User.Login");
    String userPassword = props.getProperty("User.Password");

    openbus.connect(userLogin, userPassword);
  }

  /**
   * {@inheritDoc}
   */
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
  public void executeServant(ClientTestContext context) {
    org.omg.CORBA.Object servant = context.servant;
    IHello hello = IHelloHelper.narrow(servant);

    hello.sayHello();
  }

  /**
   * {@inheritDoc}
   */
  public void disconnect(ClientTestContext context) {
    Openbus openbus = Openbus.getInstance();
    openbus.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  public void destroy(ClientTestContext context) {
    Openbus openbus = Openbus.getInstance();
    openbus.destroy();
  }
}
