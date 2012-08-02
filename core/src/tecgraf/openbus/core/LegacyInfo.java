package tecgraf.openbus.core;

import scs.core.IComponent;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlServiceHelper;

/**
 * Informações do suporte legado.
 * 
 * @author Tecgraf
 */
final class LegacyInfo {

  /** Referência para o componente legado */
  private IComponent bus;
  /** Referência para o serviço de controle de acesso legado. */
  private IAccessControlService accessControl;

  /**
   * Construtor.
   * 
   * @param bus referência para o componente legado.
   */
  LegacyInfo(IComponent bus) {
    this.bus = bus;

    org.omg.CORBA.Object obj =
      this.bus.getFacet(IAccessControlServiceHelper.id());
    this.accessControl = IAccessControlServiceHelper.narrow(obj);
  }

  /**
   * Recupera a referência do controle de acesso legado
   * 
   * @return o controle de acesso legado.
   */
  IAccessControlService getAccessControl() {
    return accessControl;
  }

}
