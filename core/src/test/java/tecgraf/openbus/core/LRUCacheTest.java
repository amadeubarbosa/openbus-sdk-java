package tecgraf.openbus.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LRUCacheTest {

  @Test
  public void addTest() {
    LRUCache<Integer, String> cache = new LRUCache<>(3);
    cache.put(1, "entry 1");
    cache.put(2, "entry 2");
    cache.put(3, "entry 3");
    cache.put(4, "entry 4");
    cache.put(5, "entry 5");
    assertFalse(cache.containsKey(1));
    assertFalse(cache.containsKey(2));
    assertTrue(cache.containsKey(3));
    assertTrue(cache.containsKey(4));
    assertTrue(cache.containsKey(5));
  }

  @Test
  public void addGetTest() {
    LRUCache<Integer, String> cache = new LRUCache<>(3);
    cache.put(1, "entry 1");
    cache.put(2, "entry 2");
    cache.put(3, "entry 3");
    cache.get(1);
    // deve sair o 2 para entrar o 4
    cache.put(4, "entry 4");
    cache.get(3);
    cache.get(1);
    // deve sair o 4 para entrar o 5
    cache.put(5, "entry 5");
    // resultado na cache deve ser 5-1-3
    assertTrue(cache.containsKey(1));
    assertFalse(cache.containsKey(2));
    assertTrue(cache.containsKey(3));
    assertFalse(cache.containsKey(4));
    assertTrue(cache.containsKey(5));
  }

  /**
   * Verificando se o contains influencia a ordem da fila.
   */
  @Test
  public void containsTest() {
    LRUCache<Integer, String> cache = new LRUCache<>(3);
    cache.put(3, "entry 3");
    cache.put(2, "entry 2");
    cache.put(1, "entry 1");
    // o loop verifica as entradas na ordem inversa de inserção.
    for (int i = 1; i < 4; i++) {
      assertTrue(cache.containsKey(i));
    }
    // espero que a entrada 3 seja a primeira a sair
    cache.put(4, "entry 4");
    // resultado na cache deve ser 4-1-2
    assertTrue(cache.containsKey(1));
    assertTrue(cache.containsKey(2));
    assertFalse(cache.containsKey(3));
    assertTrue(cache.containsKey(4));
  }
}
