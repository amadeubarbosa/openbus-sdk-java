package tecgraf.openbus.core.interceptor;

import java.util.Arrays;

import org.omg.IOP.TaggedProfile;

public class EffectiveProfile {

  public byte[] profile_data;

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

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.profile_data);
  }

}
