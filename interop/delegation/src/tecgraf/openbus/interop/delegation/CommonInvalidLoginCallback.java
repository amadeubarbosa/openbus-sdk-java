package tecgraf.openbus.interop.delegation;

import java.security.interfaces.RSAPrivateKey;

import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;

public class CommonInvalidLoginCallback implements InvalidLoginCallback {
  private String entity;
  private RSAPrivateKey privKey;
  private IComponent ic;
  private ServiceProperty[] properties;

  public CommonInvalidLoginCallback(String entity, RSAPrivateKey privKey,
    IComponent ic, ServiceProperty[] properties) {
    this.entity = entity;
    this.privKey = privKey;
    this.ic = ic;
    this.properties = properties;
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
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

}
