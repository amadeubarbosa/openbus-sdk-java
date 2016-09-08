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
 * Classe utiliária para agrupar as informações do barramento.
 * 
 * @author Tecgraf
 */
final class BusInfo {
  /** Referência CORBA::Object do barramento */
  private final org.omg.CORBA.Object rawObject;

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
  /** Referência para o registro de ofertas do barramento */
  private OfferRegistry offerRegistry;

  /** Lock para controle de concorrência no acesso ao barramento */
  private final Object lock = new Object();

  /**
   * Construtor.
   * 
   * @param obj referência para o barramento.
   */
  BusInfo(org.omg.CORBA.Object obj) {
    this.rawObject = obj;
  }

  /**
   * Obtém as referências básicas para realizar o login com o barramento.
   */
  void basicBusInitialization() {
    IComponent ic;
    synchronized (lock) {
      ic = this.bus;
    }

    if (ic == null) {
      ic = IComponentHelper.narrow(rawObject);
      if (ic == null) {
        throw new OBJECT_NOT_EXIST("Referência obtida não corresponde a um " +
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
        throw new OpenBusInternalException("Erro ao obter a chave pública do " +
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
   * Apaga a informação de identificador e chave do barramento.
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
   * Recupera a chave pública do barramento.
   * 
   * @return a chave pública do barramento.
   */
  RSAPublicKey getPublicKey() {
    synchronized (lock) {
      return publicKey;
    }
  }

  /**
   * Recupera a referência para o controle de acesso do barramento.
   * 
   * @return o controle de acesso do barramento.
   */
  AccessControl getAccessControl() {
    synchronized (lock) {
      return accessControl;
    }
  }

  /**
   * Recupera a referência para o registro de login do barramento.
   * 
   * @return o registro de login do barramento.
   */
  LoginRegistry getLoginRegistry() {
    synchronized (lock) {
      return loginRegistry;
    }
  }

  /**
   * Recupera a referência para o registro de ofertas do barramento.
   * 
   * @return o registro de ofertas do barramento.
   */
  OfferRegistry getOfferRegistry() {
    synchronized (lock) {
      return offerRegistry;
    }
  }

  /**
   * Recupera a referência para a faceta {@link IComponent}
   * 
   * @return a faceta do componente.
   */
  IComponent getComponent() {
    synchronized (lock) {
      return bus;
    }
  }

}
