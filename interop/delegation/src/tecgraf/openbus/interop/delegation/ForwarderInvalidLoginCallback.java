package tecgraf.openbus.interop.delegation;

import java.security.interfaces.RSAPrivateKey;

import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.interop.delegation.ForwarderImpl.Timer;

public class ForwarderInvalidLoginCallback implements InvalidLoginCallback {
  private String entity;
  private RSAPrivateKey privKey;
  private IComponent ic;
  private ServiceProperty[] properties;
  private Timer timer;

  public ForwarderInvalidLoginCallback(String entity, RSAPrivateKey privKey,
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
      conn.loginByCertificate(entity, privKey);
      if (conn.login() != null) {
        OpenBusContext context =
          (OpenBusContext) conn.orb().resolve_initial_references(
            "OpenBusContext");
        context.getOfferRegistry().registerService(ic, properties);
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
