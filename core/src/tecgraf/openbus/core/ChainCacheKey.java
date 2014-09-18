package tecgraf.openbus.core;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import tecgraf.openbus.core.v2_1.credential.SignedData;

/**
 * Chave da cache de cadeias assinadas.
 * 
 * @author Tecgraf
 */
class ChainCacheKey {
  /**
   * O prório login
   */
  private String login;
  /**
   * O login do alvo da requisição
   */
  private String callee;
  /**
   * A cadeia com a qual esta joined
   */
  private SignedData joinedChain;

  /**
   * Construtor.
   * 
   * @param login login.
   * @param callee login do alvo da requisição
   * @param joinedChain cadeia que esta joined
   */
  public ChainCacheKey(String login, String callee, SignedData joinedChain) {
    this.login = login;
    this.callee = callee;
    this.joinedChain = joinedChain;
  }

  /**
   * {@inheritDoc}
   */
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
    return new EqualsBuilder().append(callee, other.callee).append(login,
      other.login).append(joinedChain.encoded, other.joinedChain.encoded)
      .append(joinedChain.signature, other.joinedChain.signature).isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(callee).append(login).append(
      joinedChain.encoded).append(joinedChain.signature).toHashCode();
  }

}
