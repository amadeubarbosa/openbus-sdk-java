package tecgraf.openbus.core;

import scs.core.IComponent;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlServiceHelper;

final class LegacyInfo {

  private IComponent bus;

  private IAccessControlService accessControl;

  LegacyInfo(IComponent bus) {
    this.bus = bus;

    org.omg.CORBA.Object obj =
      this.bus.getFacet(IAccessControlServiceHelper.id());
    this.accessControl = IAccessControlServiceHelper.narrow(obj);
  }

  IAccessControlService getAccessControl() {
    return accessControl;
  }

}
