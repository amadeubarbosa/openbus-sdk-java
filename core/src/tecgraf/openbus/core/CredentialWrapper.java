package tecgraf.openbus.core;

import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v2_00.credential.CredentialData;

class CredentialWrapper {

  boolean isLegacy;
  CredentialData credential;
  Credential legacyCredential;

  public CredentialWrapper(boolean isLegacy, CredentialData credential,
    Credential legacyCredential) {
    this.isLegacy = isLegacy;
    this.credential = credential;
    this.legacyCredential = legacyCredential;
  }

  public CredentialWrapper() {
    this.isLegacy = false;
    this.credential = null;
    this.legacyCredential = null;
  }

}
