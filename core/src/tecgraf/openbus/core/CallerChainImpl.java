package tecgraf.openbus.core;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Bus;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

public final class CallerChainImpl implements CallerChain {
  private Bus bus;
  private LoginInfo[] callers;

  CallerChainImpl(Bus bus, LoginInfo[] callers) {
    this.bus = bus;
    this.callers = callers;
  }

  @Override
  public Bus getBus() {
    return this.bus;
  }

  @Override
  public LoginInfo[] callers() {
    return this.callers;
  }
}
