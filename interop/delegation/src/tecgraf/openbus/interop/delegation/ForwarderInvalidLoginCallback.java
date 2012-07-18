package tecgraf.openbus.interop.delegation;

import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.delegation.ForwarderImpl.Timer;

public class ForwarderInvalidLoginCallback implements InvalidLoginCallback {
  private String entity;
  private byte[] privKey;
  private IComponent ic;
  private ServiceProperty[] properties;
  private Timer timer;

  public ForwarderInvalidLoginCallback(String entity, byte[] privKey,
    IComponent ic, ServiceProperty[] properties, Timer timer) {
    this.entity = entity;
    this.privKey = privKey;
    this.ic = ic;
    this.properties = properties;
    this.timer = timer;
  }

  @Override
  public void invalidLogin(Connection conn, LoginInfo login) {
    try {
      System.out
        .println("Callback de InvalidLogin foi chamada, tentando logar novamente no barramento.");
      conn.loginByCertificate(entity, privKey);
      if (conn.login() != null) {
        conn.offers().registerService(ic, properties);
      }
      else {
        timer.stopTimer();
      }
    }
    catch (Exception e) {
      timer.stopTimer();
      e.printStackTrace();
    }

  }
}
