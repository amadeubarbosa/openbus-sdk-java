package tecgraf.openbus.interop.simple;

import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.PrivateKey;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

public class HelloInvalidLoginCallback implements InvalidLoginCallback {
  private String entity;
  private PrivateKey privKey;

  public HelloInvalidLoginCallback(String entity, PrivateKey privKey) {
    this.entity = entity;
    this.privKey = privKey;
  }

  @Override
  public void invalidLogin(Connection conn, LoginInfo login) {
    try {
      System.out
        .println("Callback de InvalidLogin foi chamada, tentando logar novamente no barramento.");
      conn.loginByCertificate(entity, privKey);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
