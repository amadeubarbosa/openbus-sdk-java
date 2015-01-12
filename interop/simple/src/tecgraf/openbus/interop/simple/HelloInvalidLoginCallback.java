package tecgraf.openbus.interop.simple;

import java.security.interfaces.RSAPrivateKey;

import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

public class HelloInvalidLoginCallback implements InvalidLoginCallback {
  private String entity;
  private RSAPrivateKey privKey;

  public HelloInvalidLoginCallback(String entity, RSAPrivateKey privKey) {
    this.entity = entity;
    this.privKey = privKey;
  }

  @Override
  public void invalidLogin(Connection conn, LoginInfo login) {
    try {
      conn.loginByCertificate(entity, privKey);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
