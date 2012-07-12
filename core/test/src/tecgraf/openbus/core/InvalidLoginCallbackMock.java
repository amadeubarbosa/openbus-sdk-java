package tecgraf.openbus.core;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

public class InvalidLoginCallbackMock implements InvalidLoginCallback {

  @Override
  public void invalidLogin(Connection conn, LoginInfo login, String busid) {
    throw new NotImplementedException();
  }

}
