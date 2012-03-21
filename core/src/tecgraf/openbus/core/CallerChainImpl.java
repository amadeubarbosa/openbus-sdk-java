package tecgraf.openbus.core;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChain;

final class CallerChainImpl implements CallerChain {
  private String busid;
  private LoginInfo[] callers;
  private SignedCallChain signedChain;

  CallerChainImpl(String busid, LoginInfo[] callers, SignedCallChain signedChain) {
    this.busid = busid;
    this.callers = callers;
    this.signedChain = signedChain;
  }

  @Override
  public String busid() {
    return this.busid;
  }

  @Override
  public LoginInfo[] callers() {
    return this.callers;
  }

  protected SignedCallChain signedCallChain() {
    return this.signedChain;
  }
}
