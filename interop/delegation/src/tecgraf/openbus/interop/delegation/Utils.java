package tecgraf.openbus.interop.delegation;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;

public class Utils {
  public static String getUser() {
    Credential callCred = Openbus.getInstance().getInterceptedCredential();
    return callCred.owner;
  }

}
