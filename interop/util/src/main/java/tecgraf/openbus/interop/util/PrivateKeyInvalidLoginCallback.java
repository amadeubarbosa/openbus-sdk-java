package tecgraf.openbus.interop.util;

import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;

import scs.core.ComponentId;
import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;

public class PrivateKeyInvalidLoginCallback implements InvalidLoginCallback {
  private String entity;
  private RSAPrivateKey privKey;
  private List<ToRegister> offers;

  public PrivateKeyInvalidLoginCallback(String entity, RSAPrivateKey privKey) {
    this.entity = entity;
    this.privKey = privKey;
    this.offers = new ArrayList<PrivateKeyInvalidLoginCallback.ToRegister>();
  }

  @Override
  public void invalidLogin(final Connection conn, LoginInfo login) {
    try {
      conn.loginByCertificate(entity, privKey);
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }
    // Dummy task for republishing
    new Thread(new Runnable() {

      @Override
      public void run() {
        ORB orb = conn.orb();
        OpenBusContext context;
        try {
          context =
            (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
        }
        catch (InvalidName e) {
          System.err.println("GRAVE: ORB não possui Context!");
          e.printStackTrace();
          return;
        }
        OfferRegistry registry = context.getOfferRegistry();
        for (ToRegister offer : offers) {
          try {
            registry.registerService(offer.component, offer.properties);
          }
          catch (Exception e) {
            ComponentId id = offer.component.getComponentId();
            String cid =
              String.format("ID (%s - %b.%b.%b - %s", id.name,
                id.major_version, id.minor_version, id.patch_version,
                id.platform_spec);
            System.err.println(String
              .format(
                "Erro ao republicar oferta.\nDesistindo de republicação: %s",
                cid));
            e.printStackTrace();
            continue;
          }
        }
      }
    }).start();
  }

  public void addOffer(IComponent comp, ServiceProperty[] props) {
    this.offers.add(new ToRegister(comp, props));
  }

  private class ToRegister {
    public IComponent component;
    public ServiceProperty[] properties;

    public ToRegister(IComponent comp, ServiceProperty[] props) {
      component = comp;
      properties = props;
    }
  }

}
