package tecgraf.openbus.core;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import org.omg.CORBA.OBJECT_NOT_EXIST;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.core.v2_1.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_1.services.access_control.AccessControlHelper;
import tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_1.services.access_control.LoginRegistryHelper;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistryHelper;
import tecgraf.openbus.exception.OpenBusInternalException;
import tecgraf.openbus.security.Cryptography;

/**
 * Classe utili�ria para agrupar as informa��es do barramento.
 * 
 * @author Tecgraf
 */
final class BusInfo {
  /** Refer�ncia CORBA::Object do barramento */
  private final org.omg.CORBA.Object rawObject;

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

  /** Lock para controle de concorr�ncia no acesso ao barramento */
  private final Object lock = new Object();

  /**
   * Construtor.
   * 
   * @param obj refer�ncia para o barramento.
   */
  BusInfo(org.omg.CORBA.Object obj) {
    this.rawObject = obj;
  }

  /**
   * Obt�m as refer�ncias b�sicas para realizar o login com o barramento.
   */
  void basicBusInitialization() {
    IComponent ic;
    synchronized (lock) {
      ic = this.bus;
    }

    if (ic == null) {
      ic = IComponentHelper.narrow(rawObject);
      if (ic == null) {
        throw new OBJECT_NOT_EXIST("Refer�ncia obtida n�o corresponde a um " +
          "IComponent.");
      }
      org.omg.CORBA.Object obj = ic.getFacet(AccessControlHelper.id());
      AccessControl ac = AccessControlHelper.narrow(obj);
      String id = ac.busid();
      X509Certificate certificate;
      try {
        certificate = Cryptography.getInstance().readX509Certificate(ac
          .certificate());
      }
      catch (Exception e) {
        throw new OpenBusInternalException("Erro ao obter a chave p�blica do " +
          "barramento.", e);
      }
      obj = ic.getFacet(LoginRegistryHelper.id());
      LoginRegistry login = LoginRegistryHelper.narrow(obj);
      obj = ic.getFacet(OfferRegistryHelper.id());
      OfferRegistry registry = OfferRegistryHelper.narrow(obj);

      synchronized (lock) {
        this.bus = ic;
        this.accessControl = ac;
        this.id = id;
        this.publicKey = (RSAPublicKey) certificate.getPublicKey();
        this.loginRegistry = login;
        this.offerRegistry = registry;
      }
    }
  }

  /**
   * Apaga a informa��o de identificador e chave do barramento.
   */
  void clearBusInfos() {
    synchronized (lock) {
      this.id = null;
      this.publicKey = null;
      this.bus = null;
      this.accessControl = null;
      this.loginRegistry = null;
      this.offerRegistry = null;
    }
  }

  /**
   * Recupera o identificador do barramento.
   * 
   * @return o identificador do barramento.
   */
  String getId() {
    synchronized (lock) {
      return id;
    }
  }

  /**
   * Recupera a chave p�blica do barramento.
   * 
   * @return a chave p�blica do barramento.
   */
  RSAPublicKey getPublicKey() {
    synchronized (lock) {
      return publicKey;
    }
  }

  /**
   * Recupera a refer�ncia para o controle de acesso do barramento.
   * 
   * @return o controle de acesso do barramento.
   */
  AccessControl getAccessControl() {
    synchronized (lock) {
      return accessControl;
    }
  }

  /**
   * Recupera a refer�ncia para o registro de login do barramento.
   * 
   * @return o registro de login do barramento.
   */
  LoginRegistry getLoginRegistry() {
    synchronized (lock) {
      return loginRegistry;
    }
  }

  /**
   * Recupera a refer�ncia para o registro de ofertas do barramento.
   * 
   * @return o registro de ofertas do barramento.
   */
  OfferRegistry getOfferRegistry() {
    synchronized (lock) {
      return offerRegistry;
    }
  }

  /**
   * Recupera a refer�ncia para a faceta {@link IComponent}
   * 
   * @return a faceta do componente.
   */
  IComponent getComponent() {
    synchronized (lock) {
      return bus;
    }
  }

}
