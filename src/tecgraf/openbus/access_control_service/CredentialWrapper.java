/*
 * $Id $
 */
package tecgraf.openbus.access_control_service;

import openbusidl.acs.Credential;

/**
 * Representa um invólucro para uma credencial.
 * 
 * O invólucro é necessário para a implementação de métodos como
 * {@link #toString()}, {@link #equals(Object)} e {@link #hashCode()}, que não
 * existem na classe {@link Credential} pois não são gerados pelo compilador de
 * IDL.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class CredentialWrapper {
  /**
   * A credential representada pelo invólucro.
   */
  private Credential credential;

  /**
   * Cria um invólucro para uma credencial.
   * 
   * @param credential A credencial.
   */
  public CredentialWrapper(Credential credential) {
    this.credential = credential;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(CredentialWrapper.class)) {
      return false;
    }
    CredentialWrapper wrapper = (CredentialWrapper) obj;
    if (!wrapper.credential.identifier.equals(this.credential.identifier)) {
      return false;
    }
    if (!wrapper.credential.owner.equals(this.credential.owner)) {
      return false;
    }
    if (!wrapper.credential.delegate.equals(this.credential.delegate)) {
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("{");
    builder.append(this.credential.identifier);
    builder.append(" - ");
    builder.append(this.credential.owner);
    if (!this.credential.delegate.isEmpty()) {
      builder.append("/");
      builder.append(this.credential.delegate);
    }
    builder.append("}");
    return builder.toString();
  }

  /**
   * Obtém a credential representada pelo invólucro.
   * 
   * @return A credential representada pelo invólucro.
   */
  public Credential getCredential() {
    return this.credential;
  }
}
