package tecgraf.openbus.core;

import tecgraf.openbus.core.v2_0.services.access_control.AccessControl;
import tecgraf.openbus.core.v2_1.services.legacy_support.LegacyConverter;

/**
 * Suporte legado do barramento
 *
 * @author Tecgraf/PUC-Rio
 */
class LegacySupport {

  /** Controle de acesso legado */
  private AccessControl access;
  //  private LoginRegistry logins;
  //  private OfferRegistry offers;
  /** Conversor legado */
  private LegacyConverter converter;

  /**
   * Construtor do suporte legado 2.0
   * 
   * @param access referência para o controle de acesso legado.
   * @param converter referência para o conversor legado.
   */
  public LegacySupport(AccessControl access, LegacyConverter converter) {
    this.access = access;
    this.converter = converter;
  }

  /**
   * Recupera o controle de acesso legado.
   * 
   * @return o controle de acesso legado.
   */
  public AccessControl access() {
    return access;
  }

  /**
   * Recupera o suporte a conversão de tipos legados
   * 
   * @return conversor legado.
   */
  public LegacyConverter converter() {
    return converter;
  }
}
