package tecgraf.openbus.demo.hello;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import org.omg.CORBA.UserException;

import scs.core.IComponent;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.util.CryptoUtils;

public class HelloClientDelegate {
  public static void main(String[] args) throws OpenBusException,
    UserException, IOException, InvalidKeyException, NoSuchAlgorithmException,
    InvalidKeySpecException, CertificateException {
    if (args.length != 1) {
      System.out.println("Use: HelloClientDelegate [delegate]");
      System.exit(1);
    }
    String delegate = args[0];

    Properties props = new Properties();
    InputStream in = HelloClient.class.getResourceAsStream("/Hello.properties");
    try {
      props.load(in);
    }
    finally {
      in.close();
    }

    String host = props.getProperty("host.name");
    String portString = props.getProperty("host.port");
    int port = Integer.valueOf(portString);

    Properties orbProps = new Properties();
    orbProps.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    orbProps.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
    Openbus bus = Openbus.getInstance();
    bus.init(args, orbProps, host, port);

    String entityName = props.getProperty("client.delegate.entity.name");
    String privateKeyFile = props.getProperty("client.delegate.private.key");
    String acsCertificateFile = props.getProperty("acs.certificate");

    RSAPrivateKey privateKey = CryptoUtils.readPrivateKey(privateKeyFile);
    X509Certificate acsCertificate =
      CryptoUtils.readCertificate(acsCertificateFile);

    IRegistryService registryService =
      bus.connect(entityName, privateKey, acsCertificate);
    if (registryService == null) {
      throw new RSUnavailableException();
    }

    String registeredBy = props.getProperty("server.entity.name");
    String[] facets = new String[] { IHelloHelper.id() };
    Property[] property =
      new Property[] { new Property("registered_by",
        new String[] { registeredBy }) };
    ServiceOffer[] servicesOffers =
      registryService.findByCriteria(facets, property);

    if (servicesOffers.length < 1) {
      System.out.println("O serviço Hello não se encontra no barramento.");
      System.exit(1);
    }
    if (servicesOffers.length > 1) {
      System.out.println("Existe mais de um serviço Hello no barramento.");
    }

    ServiceOffer serviceOffer = servicesOffers[0];
    IComponent component = serviceOffer.member;
    org.omg.CORBA.Object helloObject = component.getFacetByName("IHello");
    IHello hello = IHelloHelper.narrow(helloObject);

    Credential credential = bus.getCredential();
    Credential newCredential =
      new Credential(credential.identifier, credential.owner, delegate);
    bus.setThreadCredential(newCredential);

    hello.sayHello();

    bus.disconnect();
    bus.destroy();
    System.out.println("FIM");
  }
}
