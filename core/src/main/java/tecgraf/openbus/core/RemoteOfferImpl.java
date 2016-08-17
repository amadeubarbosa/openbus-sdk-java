package tecgraf.openbus.core;

import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import scs.core.IComponent;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.OfferObserver;
import tecgraf.openbus.OfferSubscription;
import tecgraf.openbus.RemoteOffer;

import java.util.HashMap;
import java.util.Map;

class RemoteOfferImpl implements RemoteOffer {
  private final Object lock = new Object();
  private final OfferRegistryImpl registry;
  private ServiceOfferDesc offer;
  private final LoginInfo owner;
  private Map<String, String> properties;

  protected RemoteOfferImpl(OfferRegistryImpl registry, ServiceOfferDesc
    offer) {
    this.registry = registry;
    this.offer = offer;
    // ao procurar o owner nas propriedades ao invés de chamar offer.ref
    // .owner(), evito fazer uma chamada remota
    this.owner = OfferRegistryImpl.getOwnerFromOffer(offer);
    this.properties = convertPropertiesToHashMap(offer.properties);
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
        ServiceProperty[] props = offer.ref.properties();
        synchronized (lock) {
          properties = convertPropertiesToHashMap(props);
          return properties;
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
    offerDesc.ref.setProperties(OfferRegistryImpl.convertMapToProperties
      (properties));
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
    try {
      offer.remove();
    } catch (OBJECT_NOT_EXIST ignored) {
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
    Map<String, String> map = new HashMap<String, String>(properties.length);
    for (ServiceProperty property : properties) {
      map.put(property.name, property.value);
    }
    return map;
  }
}