package tecgraf.openbus.interop.simple;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

public class HelloInvalidLoginCallback implements InvalidLoginCallback {
  private String entity;
  private byte[] privKey;
  private ConnectionManager manager;

  public HelloInvalidLoginCallback(String entity, byte[] privKey,
    ConnectionManager manager) {
    this.entity = entity;
    this.privKey = privKey;
    this.manager = manager;
  }

  @Override
  public void invalidLogin(Connection conn, LoginInfo login, String busid) {
    manager.setRequester(conn);
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