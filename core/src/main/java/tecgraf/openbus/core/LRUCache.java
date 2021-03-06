package tecgraf.openbus.core;

import java.util.LinkedHashMap;

/**
 * Cache LRU
 * 
 * @author Tecgraf
 * @param <k> Tipo da chave do mapa
 * @param <v> Tipo do valor do mapa
 */
class LRUCache<k, v> extends LinkedHashMap<k, v> {

  /**
   * Tamanho m�ximo da cache
   */
  private final int MAX_SIZE;

  /**
   * Construtor.
   * 
   * @param size tamanho m�ximo da cache
   */
  public LRUCache(int size) {
    // Fator de 0.75 � o default especificado na API.
    // 16 � a capacidade incial default especificado na API
    super(16, 0.75f, true);
    this.MAX_SIZE = size;
  }

  @Override
  protected boolean removeEldestEntry(java.util.Map.Entry<k, v> eldest) {
    return super.size() > MAX_SIZE;
  }

}
