package tecgraf.openbus.core;

import java.security.interfaces.RSAPublicKey;

import org.omg.CORBA.OBJECT_NOT_EXIST;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.core.v2_0.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_0.services.access_control.AccessControlHelper;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistryHelper;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistryHelper;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.security.Cryptography;

/**
 * Classe utili�ria para agrupar as informa��es do barramento.
 * 
 * @author Tecgraf
 */
final class BusInfo {

  /** Refer�ncia CORBA::Object do barramento */
  private org.omg.CORBA.Object rawObject;

  /** Identificador do barramento */
  private String id;
  /** Chave p�blica do barramento */
  private RSAPublicKey publicKey;
  /** Refer�ncia para o barramento */
  private IComponent bus;

  /** Refer�ncia para o controle de acesso do barramento */
  private AccessControl accessControl;
  /** Refer�ncia para o registro de login do barramento */
  private LoginRegistry loginRegistry;
  /** Refer�ncia para o registro de ofertas do barramento */
  private OfferRegistry offerRegistry;

  /** Lock para cotrole de concorr�ncia no acesso ao registro de logins */
  private Object lockLogin = new Object();
  /** Lock para cotrole de concorr�ncia no acesso ao registro de certificados */
  private Object lockCert = new Object();
  /** Lock para cotrole de concorr�ncia no acesso ao registro de ofertas */
  private Object lockOffer = new Object();

  /**
   * Construtor.
   * 
   * @param obj refer�ncia para o barramento.
   */
  BusInfo(org.omg.CORBA.Object obj) {
    this.rawObject = obj;
    if (rawObject == null) {
      throw new OpenBusInternalException(
        "Refer�ncia inv�lida para o barramento.");
    }
  }

  /**
   * Obt�m as refer�ncias b�sicas para realizar o login com o barramento.
   */
  void basicBusInitialization() {
    boolean existent = false;
    try {
      if (rawObject != null && !rawObject._non_existent()) {
        existent = true;
      }
    }
    catch (OBJECT_NOT_EXIST e) {
      existent = false;
    }
    if (!existent) {
      throw new OpenBusInternalException("Barramento n�o esta acess�vel.");
    }

    if (rawObject._is_a(IComponentHelper.id())) {
      this.bus = IComponentHelper.narrow(rawObject);
    }
    if (this.bus == null) {
      throw new OpenBusInternalException(
        "Refer�ncia obtida n�o corresponde a um IComponent.");
    }
    org.omg.CORBA.Object obj = this.bus.getFacet(AccessControlHelper.id());
    this.accessControl = AccessControlHelper.narrow(obj);
    retrieveBusIdAndKey();
  }

  /**
   * Atualiza a informa��o de identificador e chave do barramento.
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
        "Erro ao construir chave p�blica do barramento.", e);
    }
  }

  /**
   * Apaga a informa��o de identificador e chave do barramento.
   */
  void clearBusInfos() {
    this.id = null;
    this.publicKey = null;
    this.bus = null;
    this.accessControl = null;
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
   * Recupera a chave p�blica do barramento.
   * 
   * @return a chave p�blica do barramento.
   */
  RSAPublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Recupera a refer�ncia para o controle de acesso do barramento.
   * 
   * @return o controle de acesso do barramento.
   */
  AccessControl getAccessControl() {
    return accessControl;
  }

  /**
   * Recupera a refer�ncia para o registro de login do barramento.
   * 
   * @return o registro de login do barramento.
   */
  LoginRegistry getLoginRegistry() {
    synchronized (lockLogin) {
      if (loginRegistry == null) {
        org.omg.CORBA.Object obj = bus.getFacet(LoginRegistryHelper.id());
        loginRegistry = LoginRegistryHelper.narrow(obj);
      }
    }
    return loginRegistry;
  }

  /**
   * Recupera a refer�ncia para o registro de ofertas do barramento.
   * 
   * @return o registro de ofertas do barramento.
   */
  OfferRegistry getOfferRegistry() {
    synchronized (lockOffer) {
      if (offerRegistry == null) {
        org.omg.CORBA.Object obj = bus.getFacet(OfferRegistryHelper.id());
        offerRegistry = OfferRegistryHelper.narrow(obj);
      }
    }
    return offerRegistry;
  }

}
