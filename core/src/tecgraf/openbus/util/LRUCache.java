package tecgraf.openbus.util;

import java.util.LinkedHashMap;

/**
 * Cache LRU
 * 
 * @author Tecgraf
 * @param <k> Tipo da chave do mapa
 * @param <v> Tipo do valor do mapa
 */
public class LRUCache<k, v> extends LinkedHashMap<k, v> {

  /**
   * Tamanho máximo da cache
   */
  private final int MAX_SIZE;

  /**
   * Construtor.
   * 
   * @param size tamanho máximo da cache
   */
  public LRUCache(int size) {
    // Fator de 0.75 é o default especificado na API.
    super(size, 0.75f, true);
    this.MAX_SIZE = size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean removeEldestEntry(java.util.Map.Entry<k, v> eldest) {
    return super.size() >= MAX_SIZE;
  }
}
