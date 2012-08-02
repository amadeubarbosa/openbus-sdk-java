package tecgraf.openbus.core;

import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v2_0.credential.CredentialData;

/**
 * Classe utiliária para encapsular o credencial em uso.
 * 
 * @author Tecgraf
 */
class CredentialWrapper {

  /** Indica se a credencial pertence ao modo legado. */
  boolean isLegacy;
  /** Credencial no formato atual. */
  CredentialData credential;
  /** Credencial no formato legado. */
  Credential legacyCredential;

  /**
   * Construtor.
   * 
   * @param isLegacy se pertence ao modo legado.
   * @param credential credencial no formato atual.
   * @param legacyCredential crendencial no formato legado.
   */
  public CredentialWrapper(boolean isLegacy, CredentialData credential,
    Credential legacyCredential) {
    this.isLegacy = isLegacy;
    this.credential = credential;
    this.legacyCredential = legacyCredential;
  }

  /**
   * Construtor.
   */
  public CredentialWrapper() {
    this.isLegacy = false;
    this.credential = null;
    this.legacyCredential = null;
  }

}
