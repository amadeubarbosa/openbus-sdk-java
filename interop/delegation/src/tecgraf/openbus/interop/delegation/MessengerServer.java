package tecgraf.openbus.interop.delegation;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.core.exception.SCSException;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.Property;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.core.v1_05.registry_service.UnathorizedFacets;
import tecgraf.openbus.exception.ACSLoginFailureException;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;
import tecgraf.openbus.exception.InvalidCredentialException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.PKIException;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.exception.ServiceUnavailableException;
import tecgraf.openbus.util.CryptoUtils;

public class MessengerServer {
  private static String registrationId;

  public static void main(String[] args) throws SCSException,
    CertificateException, InvalidKeyException, NoSuchAlgorithmException,
    InvalidKeySpecException, IOException, UserException,
    ACSUnavailableException, ACSLoginFailureException,
    ServiceUnavailableException, PKIException, InvalidCredentialException,
    CORBAException, OpenBusException {

    Properties props = new Properties();
    InputStream in =
      MessengerServer.class.getResourceAsStream("/test.properties");
    if (in != null) {
      try {
        props.load(in);
      }
      finally {
        in.close();
      }
    }
    else {
      System.out
        .println("Erro ao abrir o arquivo de configuração test.properties.");
      System.exit(-1);
    }

    String host = props.getProperty("bus.host.name");
    String portString = props.getProperty("bus.host.port");
    int port = Integer.valueOf(portString);

    Properties orbProps = new Properties();
    orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
    Openbus bus = Openbus.getInstance();

    bus.initWithFaultTolerance(args, orbProps, host, port);

    String entityName = props.getProperty("messenger");
    String privateKeyFile = props.getProperty("private.key");
    String acsCertificateFile = props.getProperty("acs.certificate");

    RSAPrivateKey privateKey = CryptoUtils.readPrivateKey(privateKeyFile);
    X509Certificate acsCertificate =
      CryptoUtils.readCertificate(acsCertificateFile);

    ORB orb = bus.getORB();

    IRegistryService registryService =
      bus.connect(entityName, privateKey, acsCertificate);
    if (registryService == null) {
      throw new RSUnavailableException();
    }

    ComponentContext context =
      new ComponentContext(orb, bus.getRootPOA(), new ComponentId("Messenger",
        (byte) 1, (byte) 0, (byte) 0, "Java"));
    context.addFacet("messenger", MessengerHelper.id(), new MessengerImpl());

    org.omg.CORBA.Object obj = context.getIComponent();
    IComponent component = IComponentHelper.narrow(obj);
    Property[] properties = new Property[1];
    properties[0] =
      new Property("offer.domain", new String[] { "Interoperability Tests" });
    ServiceOffer serviceOffer = new ServiceOffer(properties, component);
    try {
      registrationId = registryService.register(serviceOffer);
      System.out.println("Messenger registrado.");
    }
    catch (UnathorizedFacets uf) {
      System.out.println("Não foi possível registrar o Messenger.");
      for (String facet : uf.facets) {
        System.out.println("Faceta '" + facet + "' não autorizada");
      }
      System.exit(1);
    }

    Runtime.getRuntime().addShutdownHook(new ShutdownThread());

    orb.run();
  }

  private static class ShutdownThread extends Thread {
    @Override
    public void run() {
      Openbus bus = Openbus.getInstance();
      IRegistryService registryService = bus.getRegistryService();
      registryService.unregister(registrationId);
      bus.disconnect();
    }
  }
}
