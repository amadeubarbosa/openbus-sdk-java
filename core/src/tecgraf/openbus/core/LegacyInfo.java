package tecgraf.openbus.core;

import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.Object;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlServiceHelper;

/**
 * Informa��es do suporte legado.
 * 
 * @author Tecgraf
 */
final class LegacyInfo {

  /**   */
  private Object rawObject;
  /** Refer�ncia para o componente legado */
  private IComponent bus;
  /** Refer�ncia para o servi�o de controle de acesso legado. */
  private IAccessControlService accessControl;

  /**
   * Construtor.
   * 
   * @param obj refer�ncia para o componente legado.
   */
  LegacyInfo(org.omg.CORBA.Object obj) {
    this.rawObject = obj;
  }

  /**
   * Recupera a refer�ncia do controle de acesso legado
   * 
   * @return o controle de acesso legado.
   */
  IAccessControlService getAccessControl() {
    return accessControl;
  }

  /**
   * Atualiza a refer�ncia para as facetas espec�ficas.
   * 
   * @return <code>true</code> caso o suporte legado foi encontrado, e
   *         <code>false</code> caso o suporte legado n�o esteja ativo.
   */
  boolean activateLegacySuport() {
    try {
      if (rawObject != null && rawObject._non_existent()) {
        return false;
      }
    }
    catch (OBJECT_NOT_EXIST e) {
      return false;
    }
    if (rawObject._is_a(IComponentHelper.id())) {
      this.bus = IComponentHelper.narrow(rawObject);
    }
    if (this.bus == null) {
      return false;
    }
    org.omg.CORBA.Object obj =
      this.bus.getFacet(IAccessControlServiceHelper.id());
    this.accessControl = IAccessControlServiceHelper.narrow(obj);
    return true;
  }
}
