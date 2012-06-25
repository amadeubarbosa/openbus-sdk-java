package tecgraf.openbus.core;

import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

import scs.core.IComponent;
import tecgraf.openbus.core.v2_00.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_00.services.access_control.AccessControlHelper;
import tecgraf.openbus.core.v2_00.services.access_control.CertificateRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.CertificateRegistryHelper;
import tecgraf.openbus.core.v2_00.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_00.services.access_control.LoginRegistryHelper;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_00.services.offer_registry.OfferRegistryHelper;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.util.Cryptography;

final class BusInfo {
  private static final Logger logger = Logger
    .getLogger(BusInfo.class.getName());

  private String id;
  private RSAPublicKey publicKey;
  private IComponent bus;

  private AccessControl accessControl;
  private LoginRegistry loginRegistry;
  private CertificateRegistry certificateRegistry;
  private OfferRegistry offerRegistry;

  BusInfo(IComponent bus) {
    this.bus = bus;

    org.omg.CORBA.Object obj = this.bus.getFacet(AccessControlHelper.id());
    this.accessControl = AccessControlHelper.narrow(obj);

    this.id = null;
    this.publicKey = null;

    obj = bus.getFacet(LoginRegistryHelper.id());
    this.loginRegistry = LoginRegistryHelper.narrow(obj);

    obj = bus.getFacet(CertificateRegistryHelper.id());
    this.certificateRegistry = CertificateRegistryHelper.narrow(obj);

    obj = bus.getFacet(OfferRegistryHelper.id());
    this.offerRegistry = OfferRegistryHelper.narrow(obj);
  }

  void retrieveBusIdAndKey() {
    this.id = this.accessControl.busid();
    try {
      this.publicKey =
        Cryptography.getInstance().generateRSAPublicKeyFromX509EncodedKey(
          this.accessControl.buskey());
    }
    catch (CryptographyException e) {
      throw new OpenBusInternalException(
        "Erro ao construir chave pública do barramento.", e);
    }
  }

  void clearBusIdAndKey() {
    this.id = null;
    this.publicKey = null;
  }

  String getId() {
    return id;
  }

  RSAPublicKey getPublicKey() {
    return publicKey;
  }

  AccessControl getAccessControl() {
    return accessControl;
  }

  LoginRegistry getLoginRegistry() {
    return loginRegistry;
  }

  CertificateRegistry getCertificateRegistry() {
    return certificateRegistry;
  }

  OfferRegistry getOfferRegistry() {
    return offerRegistry;
  }

}
