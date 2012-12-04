package tecgraf.openbus.core;

import java.util.Collections;
import java.util.Map;

import tecgraf.openbus.core.v1_05.access_control_service.Credential;

/**
 * Cache de verifica��o de validade de login.
 * 
 * @author Tecgraf
 */
class IsValidCache {

  /**
   * O mapa da cache de logins.
   */
  private Map<IsValidKey, Boolean> cache;

  /**
   * Construtor.
   * 
   * @param cacheSize tamanho da cache.
   */
  IsValidCache(int cacheSize) {
    this.cache =
      Collections.synchronizedMap(new LRUCache<IsValidKey, Boolean>(cacheSize));
  }

  /**
   * Verifica se a credencial 1.5 recebida � v�lida. A conex�o passada como
   * par�metro ser� utilizada para fazer a chamada remota para o barramento,
   * caso a informa��o n�o esteja na cache ou esteja inv�lida.
   * 
   * @param credential a credencial a ser validada
   * @param conn a conex�o a ser utilizada na chamada remota
   * @return indica��o se a credencial � v�lida ou n�o.
   */
  boolean isValid(Credential credential, ConnectionImpl conn) {
    IsValidKey key = new IsValidKey(credential.identifier, credential.owner);
    Boolean valid = this.cache.get(key);
    if (valid == null) {
      if (conn.legacy()) {
        valid = conn.legacyAccess().isValid(credential);
        this.cache.put(key, valid);
      }
      else {
        valid = false;
      }
    }
    return valid;
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
     * Recupera a refer�ncia do objeto.
     * 
     * @return o pr�prio objeto.
     */
    private IsValidCache getOuterType() {
      return IsValidCache.this;
    }

  }

  /**
   * Limpa a cache de valida��o de login.
   */
  public void clear() {
    this.cache.clear();
  }

}
