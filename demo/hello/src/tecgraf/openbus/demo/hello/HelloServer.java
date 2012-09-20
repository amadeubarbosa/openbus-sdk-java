package tecgraf.openbus.demo.hello;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
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
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.util.CryptoUtils;
import tecgraf.openbus.interop.simple.HelloHelper;

public class HelloServer {

  /**
   * Identificador da oferta.
   */
  private static String registrationId;

  public static void main(String[] args) throws IOException, UserException,
    GeneralSecurityException, OpenBusException, SCSException {
    Properties props = new Properties();
    InputStream in = HelloClient.class.getResourceAsStream("/Hello.properties");
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
        .println("Erro ao abrir o arquivo de configuração Hello.properties.");
      System.exit(-1);
    }

    String host = props.getProperty("host.name");
    String portString = props.getProperty("host.port");
    int port = Integer.valueOf(portString);

    Properties orbProps = new Properties();
    orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
    Openbus bus = Openbus.getInstance();

    bus.initWithFaultTolerance(args, orbProps, host, port);

    String entityName = props.getProperty("server.entity.name");
    String privateKeyFile = props.getProperty("server.private.key");
    String acsCertificateFile = props.getProperty("acs.certificate");

    RSAPrivateKey privateKey = CryptoUtils.readPrivateKey(privateKeyFile);
    X509Certificate acsCertificate =
      CryptoUtils.readCertificate(acsCertificateFile);

    ORB orb = bus.getORB();

    // Cria o componente.
    ComponentContext context =
      new ComponentContext(orb, bus.getRootPOA(), new ComponentId("Hello",
        (byte) 1, (byte) 0, (byte) 0, "Java"));
    context.addFacet("IHello", HelloHelper.id(), new HelloImpl());

    IRegistryService registryService =
      bus.connect(entityName, privateKey, acsCertificate);
    if (registryService == null) {
      throw new RSUnavailableException();
    }

    System.out.println("Hello Server conectado.");

    org.omg.CORBA.Object obj = context.getIComponent();
    IComponent component = IComponentHelper.narrow(obj);
    Property[] properties = new Property[0];
    ServiceOffer serviceOffer = new ServiceOffer(properties, component);
    try {
      registrationId = registryService.register(serviceOffer);
      System.out.println("Hello Server registrado.");
    }
    catch (UnathorizedFacets uf) {
      System.out.println("Não foi possível registrar Hello Server.");
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
