package tecgraf.openbus.test_case;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.Property;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.core.v1_05.registry_service.UnathorizedFacets;
import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.launcher.HelloServant;
import tecgraf.openbus.launcher.ServerTestContext;
import tecgraf.openbus.util.CryptoUtils;
import testidl.hello.IHelloHelper;

/**
 * <p>
 * Classe responsável por implementar a interface ServerTestCase de forma
 * padrão.
 * </p>
 * <p>
 * Replica o comportamento do demo hello.
 * </p>
 * 
 * @author Tecgraf
 */
public class DefaultServerTestCase implements ServerTestCase {

  /**
   * {@inheritDoc}
   */
  public void init(ServerTestContext context) throws OpenBusException,
    UserException {
    Openbus openbus = Openbus.getInstance();
    Properties props = context.properties;
    String host = props.getProperty("Host.Name");
    String portString = props.getProperty("Host.Port");
    int port = Integer.valueOf(portString);

    Properties orbProps = new Properties();
    orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");

    openbus.initWithFaultTolerance(null, orbProps, host, port);
  }

  /**
   * {@inheritDoc}
   */
  public void connect(ServerTestContext context) throws OpenBusException {
    Openbus openbus = Openbus.getInstance();
    Properties props = context.properties;
    String entityName = props.getProperty("Server.EntityName");
    String privateKeyFile = props.getProperty("Server.Key");
    String acsCertificateFile = props.getProperty("Acs.Certificate");

    RSAPrivateKey privateKey = null;
    X509Certificate acsCertificate = null;
    try {
      privateKey = CryptoUtils.readPrivateKey(privateKeyFile);
      acsCertificate = CryptoUtils.readCertificate(acsCertificateFile);
    }
    catch (Exception e) {
      throw new ACSLoginFailureException("Erro na manipulação das chaves.", e);
    }

    openbus.connect(entityName, privateKey, acsCertificate);
  }

  /**
   * {@inheritDoc}
   */
  public void createComponent(ServerTestContext context) throws Exception {
    Openbus openbus = Openbus.getInstance();
    ORB orb = openbus.getORB();

    context.componentContext =
      new ComponentContext(orb, openbus.getRootPOA(), new ComponentId("Hello",
        (byte) 1, (byte) 0, (byte) 0, "Java"));
    context.componentContext.addFacet("IHello", IHelloHelper.id(),
      new HelloServant());
  }

  /**
   * {@inheritDoc}
   */
  public String registerComponent(ServerTestContext context)
    throws UnathorizedFacets {
    Openbus openbus = Openbus.getInstance();
    ComponentContext componentContext = context.componentContext;
    IRegistryService registryService = openbus.getRegistryService();

    org.omg.CORBA.Object obj = componentContext.getIComponent();
    IComponent component = IComponentHelper.narrow(obj);
    Property registrationProps[] = new Property[1];
    registrationProps[0] = new Property();
    registrationProps[0].name = "facets";
    registrationProps[0].value = new String[1];
    registrationProps[0].value[0] = IHelloHelper.id();
    ServiceOffer serviceOffer = new ServiceOffer(registrationProps, component);

    return registryService.register(serviceOffer);
  }

  /**
   * {@inheritDoc}
   */
  public void disconnect(ServerTestContext context) {
    Openbus openbus = Openbus.getInstance();
    openbus.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  public void destroy(ServerTestContext context) {
    Openbus openbus = Openbus.getInstance();
    openbus.destroy();
  }
}
