package tecgraf.openbus.core;

import java.util.Collections;
import java.util.Map;

/**
 * Cache de verificação de validade de login.
 * 
 * @author Tecgraf
 */
class IsValidCache {

  /**
   * O mapa da cache de logins.
   */
  private Map<IsValidKey, Boolean> cache;
  /**
   * A conexão ao qual o cache esta associado.
   */
  private ConnectionImpl conn;

  /**
   * Construtor.
   * 
   * @param conn a conexão ao qual o cache esta associado.
   * @param cacheSize tamanho da cache.
   */
  IsValidCache(ConnectionImpl conn, int cacheSize) {
    this.conn = conn;
    this.cache =
      Collections.synchronizedMap(new LRUCache<IsValidKey, Boolean>(cacheSize));
  }

  /**
   * Classe privada que atua como chave do mapa da cache.
   * 
   * @author Tecgraf
   */
  private class IsValidKey {

    /** Identificador de login da entidade */
    public String id;
    /** Nome da entidade */
    public String owner;

    /**
     * Construtor.
     * 
     * @param id identificador do login da entidade
     * @param owner nome da entidade
     */
    public IsValidKey(String id, String owner) {
      super();
      this.id = id;
      this.owner = owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((owner == null) ? 0 : owner.hashCode());
      return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      IsValidKey other = (IsValidKey) obj;
      if (!getOuterType().equals(other.getOuterType())) {
        return false;
      }
      if (id == null) {
        if (other.id != null) {
          return false;
        }
      }
      else if (!id.equals(other.id)) {
        return false;
      }
      if (owner == null) {
        if (other.owner != null) {
          return false;
        }
      }
      else if (!owner.equals(other.owner)) {
        return false;
      }
      return true;
    }

    /**
     * Recupera a referência do objeto.
     * 
     * @return o próprio objeto.
     */
    private IsValidCache getOuterType() {
      return IsValidCache.this;
    }

  }

  /**
   * Limpa a cache de validação de login.
   */
  public void clear() {
    this.cache.clear();
  }

}
