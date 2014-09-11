package tecgraf.openbus.core;

import java.util.Arrays;

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
    if (obj instanceof ChainCacheKey) {
      ChainCacheKey other = (ChainCacheKey) obj;
      if (this.callee.equals(other.callee)
        && this.login.equals(other.login)
        && Arrays.equals(this.joinedChain.encoded, other.joinedChain.encoded)
        && Arrays.equals(this.joinedChain.signature,
          other.joinedChain.signature)) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    // um valor qualquer
    int BASE = 17;
    // um valor qualquer
    int SEED = 37;
    int hash = BASE;
    if (this.login != null) {
      hash = hash * SEED + this.login.hashCode();
    }
    if (this.callee != null) {
      hash = hash * SEED + this.login.hashCode();
    }
    hash += Arrays.hashCode(this.joinedChain.encoded);
    hash += Arrays.hashCode(this.joinedChain.signature);
    return hash;
  }

}
