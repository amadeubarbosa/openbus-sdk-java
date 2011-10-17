package tecgraf.openbus;

import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

public interface CallerChain {
  Bus getBus();

  LoginInfo[] getCallers();
}
