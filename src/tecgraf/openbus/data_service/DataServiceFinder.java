/*
 * $Id$
 */
package tecgraf.openbus.data_service;

import openbusidl.rs.IRegistryService;
import openbusidl.rs.Property;
import openbusidl.rs.ServiceOffer;

import org.omg.CORBA.ORB;

import scs.core.ComponentId;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.Openbus;

/**
 * Utilitário que realiza uma busca por um Serviço de Dados.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class DataServiceFinder {
  /**
   * Realiza uma busca por um Serviço de Dados a partir da chave de um dado. O
   * dado representado pela chave é oriundo do Serviço de Dados encontrado.
   * 
   * @param dataKey A chave do dado que contém as informações sobre o Serviço de
   *        Dados procurado.
   * 
   * @return O Serviço de Dados.
   * 
   * @throws DataServiceUnavailableException Caso o Serviço de Dados não seja
   *         encontrado.
   */
  public static IComponent find(DataKey dataKey)
    throws DataServiceUnavailableException {
    Openbus bus = Openbus.getInstance();

    org.omg.CORBA.Object obj;
    String serviceFacetIOR = dataKey.getServiceFacetIOR();
    if (serviceFacetIOR != null) {
      ORB orb = bus.getORB();
      obj = orb.string_to_object(serviceFacetIOR);
      obj = obj._get_component();
      return IComponentHelper.narrow(obj);
    }

    IRegistryService registryService = bus.getRegistryService();
    String[] facets = new String[] { dataKey.getServiceInterfaceName() };

    ServiceOffer[] serviceOffer;
    ComponentId componentId = dataKey.getServiceComponentId();
    if (componentId != null) {
      String componentIdString = componentId.name + ":";
      componentIdString += componentId.major_version + ".";
      componentIdString += componentId.minor_version + ".";
      componentIdString += componentId.patch_version;

      Property property =
        new Property("component_id", new String[] { componentIdString });
      serviceOffer =
        registryService.findByCriteria(facets, new Property[] { property });
    }
    serviceOffer = registryService.find(facets);
    if (serviceOffer.length < 1) {
      throw new DataServiceUnavailableException();
    }
    return serviceOffer[0].member;
  }
}
