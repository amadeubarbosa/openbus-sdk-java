package tecgraf.openbus.defaultimpl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import scs.core.IComponent;
import tecgraf.openbus.Bus;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionObserver;
import tecgraf.openbus.CryptographyException;
import tecgraf.openbus.core.v2_00.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_00.services.access_control.AccessControlHelper;
import tecgraf.openbus.core.v2_00.services.access_control.CertificateRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.CertificateRegistryHelper;
import tecgraf.openbus.core.v2_00.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.LoginRegistryHelper;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistryHelper;

public final class BusImpl implements Bus, ConnectionObserver {
  private static final Logger logger = Logger
    .getLogger(BusImpl.class.getName());

  private String id;
  private BusORB orb;
  private IComponent bus;

  private AccessControl accessControl;
  private LoginRegistry loginRegistry;
  private CertificateRegistry certificateRegistry;
  private OfferRegistry offerRegistry;

  private Set<Connection> connections;

  public BusImpl(BusORB orb, IComponent bus) {
    this.orb = orb;
    this.bus = bus;
    this.connections = new HashSet<Connection>();

    org.omg.CORBA.Object obj = this.bus.getFacet(AccessControlHelper.id());
    this.accessControl = AccessControlHelper.narrow(obj);
    this.id = this.accessControl.busid();

    obj = bus.getFacet(LoginRegistryHelper.id());
    this.loginRegistry = LoginRegistryHelper.narrow(obj);

    obj = bus.getFacet(CertificateRegistryHelper.id());
    this.certificateRegistry = CertificateRegistryHelper.narrow(obj);

    obj = bus.getFacet(OfferRegistryHelper.id());
    this.offerRegistry = OfferRegistryHelper.narrow(obj);
  }

  @Override
  public BusORB getORB() {
    return this.orb;
  }

  @Override
  public String getId() {
    return this.id;
  }

  public Connection createConnection() throws CryptographyException {
    Connection connection = new ConnectionImpl(this);
    connection.addObserver(this);
    this.connections.add(connection);
    return connection;
  }

  @Override
  public void connectionClosed(Connection connection) {
    this.connections.remove(connections);
  }

  @Override
  public Collection<Connection> getConnections() {
    return Collections.unmodifiableCollection(this.connections);
  }

  AccessControl getAccessControl() {
    return this.accessControl;
  }

  LoginRegistry getLoginRegistry() {
    return this.loginRegistry;
  }

  CertificateRegistry getCertificateRegistry() {
    return this.certificateRegistry;
  };

  OfferRegistry getOfferRegistry() {
    return this.offerRegistry;
  }
}
