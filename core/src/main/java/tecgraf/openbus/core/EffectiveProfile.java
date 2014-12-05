package tecgraf.openbus.core;

import java.util.Arrays;

import org.omg.IOP.TaggedProfile;

/**
 * Guarda a informa��o de perfil da requisi��o.
 * 
 * @author Tecgraf
 */
class EffectiveProfile {

  /**
   * Informa��o do profiler
   */
  private byte[] profile_data;

  /**
   * Construtor.
   * 
   * @param effective_profile informa��o de perfil da requisi��o
   */
  public EffectiveProfile(TaggedProfile effective_profile) {
    this.profile_data = effective_profile.profile_data;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof EffectiveProfile) {
      EffectiveProfile ep = (EffectiveProfile) obj;
      return Arrays.equals(this.profile_data, ep.profile_data);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Arrays.hashCode(this.profile_data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new String(this.profile_data);
  }

}
