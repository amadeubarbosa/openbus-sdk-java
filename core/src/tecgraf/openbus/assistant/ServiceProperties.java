package tecgraf.openbus.assistant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;

public class ServiceProperties {

  HashMap<String, ServiceProperty> map;

  public ServiceProperties(ServiceProperty[] properties) {
    this.map = new HashMap<String, ServiceProperty>();
    for (ServiceProperty prop : properties) {
      map.put(prop.name, prop);
    }
  }

  public List<ServiceProperty> getList() {
    return new ArrayList<ServiceProperty>(this.map.values());
  }

  public String getProperty(String property) {
    ServiceProperty serviceProperty = map.get(property);
    if (serviceProperty != null) {
      return serviceProperty.value;
    }
    return null;
  }
}
