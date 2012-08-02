package tecgraf.openbus.core;

import java.security.interfaces.RSAPublicKey;

import scs.core.IComponent;
import tecgraf.openbus.core.v2_0.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_0.services.access_control.AccessControlHelper;
import tecgraf.openbus.core.v2_0.services.access_control.CertificateRegistry;
import tecgraf.openbus.core.v2_0.services.access_control.CertificateRegistryHelper;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistryHelper;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistryHelper;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.util.Cryptography;

/**
 * Classe utiliária para agrupar as informações do barramento.
 * 
 * @author Tecgraf
 */
final class BusInfo {

  /** Identificador do barramento */
  private String id;
  /** Chave pública do barramento */
  private RSAPublicKey publicKey;
  /** Referência para o barramento */
  private IComponent bus;

  /** Referência para o controle de acesso do barramento */
  private AccessControl accessControl;
  /** Referência para o registro de login do barramento */
  private LoginRegistry loginRegistry;
  /** Referência para o registro de certificado do barramento */
  private CertificateRegistry certificateRegistry;
  /** Referência para o registro de ofertas do barramento */
  private OfferRegistry offerRegistry;

  /**
   * Construtor.
   * 
   * @param bus referência para o barramento.
   */
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

    retrieveBusIdAndKey();
  }

  /**
   * Atualiza a informação de identificador e chave do barramento.
   */
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

  /**
   * Apaga a informação de identificador e chave do barramento.
   */
  void clearBusIdAndKey() {
    this.id = null;
    this.publicKey = null;
  }

  /**
   * Recupera o identificador do barramento.
   * 
   * @return o identificador do barramento.
   */
  String getId() {
    return id;
  }

  /**
   * Recupera a chave pública do barramento.
   * 
   * @return a chave pública do barramento.
   */
  RSAPublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Recupera a referência para o controle de acesso do barramento.
   * 
   * @return o controle de acesso do barramento.
   */
  AccessControl getAccessControl() {
    return accessControl;
  }

  /**
   * Recupera a referência para o registro de login do barramento.
   * 
   * @return o registro de login do barramento.
   */
  LoginRegistry getLoginRegistry() {
    return loginRegistry;
  }

  /**
   * Recupera a referência para o registro de certificado do barramento.
   * 
   * @return o registro de certificado do barramento.
   */
  CertificateRegistry getCertificateRegistry() {
    return certificateRegistry;
  }

  /**
   * Recupera a referência para o registro de ofertas do barramento.
   * 
   * @return o registro de ofertas do barramento.
   */
  OfferRegistry getOfferRegistry() {
    return offerRegistry;
  }

}
