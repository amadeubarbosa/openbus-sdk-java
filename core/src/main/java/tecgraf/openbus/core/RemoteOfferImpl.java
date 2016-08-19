package tecgraf.openbus.core;

import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import scs.core.IComponent;
import tecgraf.openbus.*;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;

import java.util.HashMap;
import java.util.Map;

class RemoteOfferImpl implements RemoteOffer {
  private final Object lock = new Object();
  private final OfferRegistryImpl registry;
  private ServiceOfferDesc offer;
  private final LoginInfo owner;
  private Map<String, String> properties;
  private final OpenBusContext context;
  private final Connection conn;

  protected RemoteOfferImpl(OfferRegistryImpl registry, ServiceOfferDesc
    offer) {
    this.registry = registry;
    this.offer = offer;
    // ao procurar o owner nas propriedades ao invés de chamar offer.ref
    // .owner(), evito fazer uma chamada remota
    this.owner = OfferRegistryImpl.getOwnerFromOffer(offer);
    this.properties = convertPropertiesToHashMap(offer.properties);
    this.conn = registry.conn();
    this.context = this.conn.context();
  }

  @Override
  public LoginInfo owner() {
    return owner;
  }

  @Override
  public IComponent service_ref() {
    synchronized (lock) {
      return offer != null ? offer.service_ref : null;
    }
  }

  @Override
  public Map<String, String> properties(boolean update) {
    if (update) {
      ServiceOfferDesc offer = offer();
      if (offer != null) {
        Connection prev = context.getCurrentConnection();
        try {
          context.setCurrentConnection(conn);
          ServiceProperty[] props = offer.ref.properties();
          synchronized (lock) {
            properties = convertPropertiesToHashMap(props);
            return properties;
          }
        } finally {
          context.setCurrentConnection(prev);
        }
      }
    }
    synchronized (lock) {
      return properties;
    }
  }

  @Override
  public void propertiesLocal(Map<String, String> properties) {
    updateProperties(properties);
  }

  @Override
  public void propertiesRemote(Map<String, String> properties) throws
    UnauthorizedOperation, InvalidProperties, ServiceFailure {
    ServiceOfferDesc offerDesc = offer();
    if (offerDesc == null || offerDesc.ref == null) {
      throw new OBJECT_NOT_EXIST("A oferta foi removida do barramento.");
    }
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      offerDesc.ref.setProperties(OfferRegistryImpl.convertMapToProperties
        (properties));
    } finally {
      context.setCurrentConnection(prev);
    }
    updateProperties(properties);
  }

  @Override
  public void remove() throws ServiceFailure, UnauthorizedOperation {
    ServiceOfferDesc offerDesc = offer();
    if (offerDesc == null) {
      return;
    }
    ServiceOffer offer = offerDesc.ref;
    if (offer == null) {
      return;
    }
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      offer.remove();
    } catch (OBJECT_NOT_EXIST ignored) {
    } finally {
      context.setCurrentConnection(prev);
    }
    removed();
  }

  @Override
  public OfferSubscription subscribeObserver(OfferObserver observer) throws
    ServantNotActive, WrongPolicy {
    return registry.subscribeToOffer(this, observer);
  }

  protected ServiceOfferDesc offer() {
    synchronized (lock) {
      return offer;
    }
  }

  protected void offer(ServiceOfferDesc offer) {
    synchronized (lock) {
      this.offer = offer;
    }
  }

  protected void removed() {
    synchronized (lock) {
      offer = null;
    }
  }

  protected void updateProperties(Map<String, String> props) {
    synchronized (lock) {
      this.properties = props;
    }
  }

  public static Map<String, String> convertPropertiesToHashMap
    (ServiceProperty[] properties) {
    Map<String, String> map = new HashMap<>(properties.length);
    for (ServiceProperty property : properties) {
      map.put(property.name, property.value);
    }
    return map;
  }
}