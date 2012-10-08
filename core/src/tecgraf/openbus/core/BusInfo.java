package tecgraf.openbus.core;

import java.security.interfaces.RSAPublicKey;

import scs.core.IComponent;
import scs.core.IComponentHelper;
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
import tecgraf.openbus.security.Cryptography;

/**
 * Classe utiliária para agrupar as informações do barramento.
 * 
 * @author Tecgraf
 */
final class BusInfo {

  /** Referência CORBA::Object do barramento */
  private org.omg.CORBA.Object rawObject;

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
   * @param obj referência para o barramento.
   */
  BusInfo(org.omg.CORBA.Object obj) {
    this.rawObject = obj;
    if (rawObject == null) {
      throw new OpenBusInternalException(
        "Referência inválida para o barramento.");
    }
  }

  /**
   * Obtém as referências básicas para realizar o login com o barramento.
   */
  void basicBusInitialization() {
    boolean existent = false;
    if (rawObject != null && !rawObject._non_existent()) {
      existent = true;
    }
    if (!existent) {
      throw new OpenBusInternalException("Barramento não esta acessível.");
    }

    if (rawObject._is_a(IComponentHelper.id())) {
      this.bus = IComponentHelper.narrow(rawObject);
    }
    if (this.bus == null) {
      throw new OpenBusInternalException(
        "Referência obtida não corresponde a um IComponent.");
    }
    org.omg.CORBA.Object obj = this.bus.getFacet(AccessControlHelper.id());
    this.accessControl = AccessControlHelper.narrow(obj);
    retrieveBusIdAndKey();
  }

  /**
   * Inicializa todas as referências das demais facetas do barramento
   */
  void fullBusInitialization() {
    org.omg.CORBA.Object obj;
    if (this.loginRegistry == null) {
      obj = bus.getFacet(LoginRegistryHelper.id());
      this.loginRegistry = LoginRegistryHelper.narrow(obj);
    }

    if (this.certificateRegistry == null) {
      obj = bus.getFacet(CertificateRegistryHelper.id());
      this.certificateRegistry = CertificateRegistryHelper.narrow(obj);
    }

    if (this.offerRegistry == null) {
      obj = bus.getFacet(OfferRegistryHelper.id());
      this.offerRegistry = OfferRegistryHelper.narrow(obj);
    }
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
  void clearBusInfos() {
    this.id = null;
    this.publicKey = null;
    this.bus = null;
    this.accessControl = null;
    this.certificateRegistry = null;
    this.loginRegistry = null;
    this.offerRegistry = null;
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
    if (loginRegistry == null) {
      fullBusInitialization();
    }
    return loginRegistry;
  }

  /**
   * Recupera a referência para o registro de certificado do barramento.
   * 
   * @return o registro de certificado do barramento.
   */
  CertificateRegistry getCertificateRegistry() {
    if (certificateRegistry == null) {
      fullBusInitialization();
    }
    return certificateRegistry;
  }

  /**
   * Recupera a referência para o registro de ofertas do barramento.
   * 
   * @return o registro de ofertas do barramento.
   */
  OfferRegistry getOfferRegistry() {
    if (offerRegistry == null) {
      fullBusInitialization();
    }
    return offerRegistry;
  }

}
