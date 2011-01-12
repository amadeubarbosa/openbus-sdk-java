package tecgraf.openbus.demo.delegate;

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
import tecgraf.openbus.core.v1_06.access_control_service.Credential;
import tecgraf.openbus.core.v1_06.registry_service.IRegistryService;
import tecgraf.openbus.core.v1_06.registry_service.Property;
import tecgraf.openbus.core.v1_06.registry_service.ServiceOffer;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.RSUnavailableException;
import tecgraf.openbus.util.CryptoUtils;
import demoidl.demoDelegate.IHello;
import demoidl.demoDelegate.IHelloHelper;

public class HelloClient {
  public static void main(String[] args) throws OpenBusException,
    UserException, IOException, InvalidKeyException, NoSuchAlgorithmException,
    InvalidKeySpecException, CertificateException {
    Properties props = new Properties();
    InputStream in =
      HelloClient.class.getResourceAsStream("/Delegate.properties");
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

    String entityName = props.getProperty("client.entity.name");
    String privateKeyFile = props.getProperty("client.private.key");
    String acsCertificateFile = props.getProperty("acs.certificate");

    RSAPrivateKey privateKey = CryptoUtils.readPrivateKey(privateKeyFile);
    X509Certificate acsCertificate =
      CryptoUtils.readCertificate(acsCertificateFile);

    IRegistryService registryService =
      bus.connect(entityName, privateKey, acsCertificate);
    if (registryService == null) {
      throw new RSUnavailableException();
    }

    String[] facets = new String[] { "IHello" };
    String[] componentName = new String[] { "DelegateService:1.0.0" };
    Property[] property =
      new Property[] { new Property("component_id", componentName) };
    ServiceOffer[] servicesOffers =
      registryService.findByCriteria(facets, property);

    if (servicesOffers.length < 1) {
      System.out.println("O serviço Hello não se encontra no barramento.");
      System.exit(1);
    }
    if (servicesOffers.length > 1)
      System.out.println("Existe mais de um serviço Hello no barramento.");

    ServiceOffer serviceOffer = servicesOffers[0];
    IComponent component = serviceOffer.member;
    org.omg.CORBA.Object helloObject = component.getFacetByName("IHello");
    IHello hello = IHelloHelper.narrow(helloObject);

    DelegateThread delegateThreadA = new DelegateThread("A", hello);
    DelegateThread delegateThreadB = new DelegateThread("B", hello);
    Thread threadA = new Thread(delegateThreadA);
    Thread threadB = new Thread(delegateThreadB);

    threadA.start();
    threadB.start();

    try {
      threadA.join();
      threadB.join();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    bus.disconnect();
    bus.destroy();
    System.out.println("FIM");
  }
}

class DelegateThread implements Runnable {

  private String name;
  private IHello hello;

  public DelegateThread(String name, IHello hello) {
    this.name = name;
    this.hello = hello;
  }

  public void run() {
    Openbus openbus = Openbus.getInstance();
    Credential credential = openbus.getCredential();
    Credential newCredential =
      new Credential(credential.identifier, credential.owner, name);
    openbus.setThreadCredential(newCredential);

    try {
      for (int i = 0; i < 10; i++) {
        hello.sayHello(name);
        Thread.sleep(1000);
      }
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
