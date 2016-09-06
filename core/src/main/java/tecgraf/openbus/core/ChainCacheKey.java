package tecgraf.openbus.core;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Chave da cache de cadeias assinadas.
 * 
 * @author Tecgraf
 */
class ChainCacheKey {
  /**
   * Se necessita de cadeia legada
   */
  private final boolean legacy;
  /**
   * Entidade do alvo da requisição
   */
  private final String callee;
  /**
   * A assinatura da cadeia com a qual esta joined
   */
  private final byte[] signature;

  /**
   * Construtor.
   * 
   * @param callee entidade do alvo da requisição
   * @param signature assinatura da cadeia que esta joined
   * @param legacy se em modo legado ou não
   */
  public ChainCacheKey(String callee, byte[] signature, boolean legacy) {
    this.callee = callee;
    this.signature = signature;
    this.legacy = legacy;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    ChainCacheKey other = (ChainCacheKey) obj;
    return new EqualsBuilder().append(callee, other.callee).append(legacy,
      other.legacy).append(signature, signature).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(callee).append(legacy)
      .append(signature).toHashCode();
  }

}
