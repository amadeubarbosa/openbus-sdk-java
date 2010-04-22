package tecgraf.openbus.demo.hello;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.logging.Level;

import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_05.registry_service.Property;
import tecgraf.openbus.core.v1_05.registry_service.ServiceOffer;
import tecgraf.openbus.core.v1_05.registry_service.UnathorizedFacets;

import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;

import scs.core.ComponentId;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.core.servant.ComponentBuilder;
import scs.core.servant.ComponentContext;
import scs.core.servant.ExtendedFacetDescription;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.util.CryptoUtils;
import tecgraf.openbus.util.Log;
import demoidl.hello.IHelloHelper;

public class HelloServer {
  public static void main(String[] args) throws IOException, UserException,
    GeneralSecurityException, SecurityException, InstantiationException,
    IllegalAccessException, ClassNotFoundException, InvocationTargetException,
    NoSuchMethodException, OpenBusException {
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
        .println("Erro ao abrir o arquivo de configura��o Hello.properties.");
      System.exit(-1);
    }

    String host = props.getProperty("host.name");
    String portString = props.getProperty("host.port");
    int port = Integer.valueOf(portString);

    Log.setLogsLevel(Level.WARNING);
    Properties orbProps = new Properties();
    orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
    Openbus bus = Openbus.getInstance();
    //bus.resetAndInitialize(args, orbProps, host, port);
    bus.initWithFaultTolerance(args, orbProps, host, port);

    String entityName = props.getProperty("entity.name");
    String privateKeyFile = props.getProperty("private.key");
    String acsCertificateFile = props.getProperty("acs.certificate");

    RSAPrivateKey privateKey = CryptoUtils.readPrivateKey(privateKeyFile);
    X509Certificate acsCertificate =
      CryptoUtils.readCertificate(acsCertificateFile);

    ORB orb = bus.getORB();

    // Cria o componente.
    ComponentBuilder builder = new ComponentBuilder(bus.getRootPOA(), orb);
    ExtendedFacetDescription[] descriptions = new ExtendedFacetDescription[1];
    descriptions[0] =
      new ExtendedFacetDescription("IHello", IHelloHelper.id(), HelloImpl.class
        .getCanonicalName());
    ComponentContext context =
      builder.newComponent(descriptions, new ComponentId("Hello", (byte) 1,
        (byte) 0, (byte) 0, "Java"));

    IRegistryService registryService =
      bus.connect(entityName, privateKey, acsCertificate);
    if (registryService == null) {
      throw new RSUnavailableException();
    }

    System.out.println("Hello Server conectado.");

    org.omg.CORBA.Object obj = context.getIComponent();
    IComponent component = IComponentHelper.narrow(obj);
    Property registrationProps[] = new Property[1];
    registrationProps[0] = new Property();
    registrationProps[0].name = "facets";
    registrationProps[0].value = new String[1];
    registrationProps[0].value[0] = "IDL:demoidl/hello/IHello:1.0";
    ServiceOffer serviceOffer = new ServiceOffer(registrationProps, component);
    try {
    	String registrationId = registryService.register(serviceOffer);
        System.out.println("Hello Server registrado.");
    } catch (UnathorizedFacets uf) {
        System.out.println("N�o foi poss�vel registrar Hello Server.");
        for (String facet : uf.facets) {
            System.out.println("Faceta '" + facet + "' n�o autorizada");
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
      bus.disconnect();
    }
  }
}
