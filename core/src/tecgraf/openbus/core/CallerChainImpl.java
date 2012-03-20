package tecgraf.openbus.core;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

final class CallerChainImpl implements CallerChain {
  private String busid;
  private LoginInfo[] callers;

  CallerChainImpl(String busid, LoginInfo[] callers) {
    this.busid = busid;
    this.callers = callers;
  }

  @Override
  public String busid() {
    return this.busid;
  }

  @Override
  public LoginInfo[] callers() {
    return this.callers;
  }
}
