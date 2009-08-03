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
 * Utilit�rio que realiza uma busca por um Servi�o de Dados.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class DataServiceFinder {
  /**
   * Realiza uma busca por um Servi�o de Dados a partir da chave de um dado. O
   * dado representado pela chave � oriundo do Servi�o de Dados encontrado.
   * 
   * @param dataKey A chave do dado que cont�m as informa��es sobre o Servi�o de
   *        Dados procurado.
   * 
   * @return O Servi�o de Dados.
   * 
   * @throws DataServiceUnavailableException Caso o Servi�o de Dados n�o seja
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
