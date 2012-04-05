package tecgraf.openbus.util;

/**
 * Histórico de tickets
 * 
 * @author Tecgraf
 */
public class TicketsHistory {

  /**
   * valor base
   */
  private int base;
  /**
   * mapa de tickets utilizados
   */
  private int bits;
  /**
   * índice corrente no mapa de bits
   */
  private int index;
  /**
   * tamanho padrão do histórico.
   */
  private static final int DEFAULT_SIZE = 32;
  /**
   * o tamanho histórico.
   */
  private final int size;

  /**
   * Construtor.
   */
  public TicketsHistory() {
    this(DEFAULT_SIZE);
  }

  /**
   * Construtor.
   * 
   * @param size tamanho do histórico.
   */
  public TicketsHistory(int size) {
    this.base = 0;
    this.bits = 0;
    this.index = 0;
    this.size = size;
  }

  /**
   * Verifica se a posição esta marcada.
   * 
   * @param index o índice verificado.
   * @return se posição esta marcada ou não.
   */
  private boolean FLAG(int index) {
    return (this.bits & (1 << index)) != 0;
  }

  /**
   * Marca a posição.
   * 
   * @param index a posição.
   */
  private void SET(int index) {
    this.bits |= (1 << index);
  }

  /**
   * Limpa a posição.
   * 
   * @param index a posição.
   */
  private void CLEAR(int index) {
    this.bits &= ~(1 << index);
  }

  /**
   * Descarta o valor de base atual e configura um novo.
   */
  private void discardBase() {
    this.base++;
    if (this.bits != 0) {
      for (int i = 0; i < this.size; i++) {
        if (!FLAG(this.index)) {
          break;
        }
        CLEAR(this.index);
        this.index = (this.index + 1) % size;
        this.base++;
      }
      this.index = (this.index + 1) % size;
    }
  }

  /**
   * Verifica se o ticket é válido e marca com utilizado caso seja válido.
   * 
   * @param id o ticket a ser utilizado.
   * @return <code>true</code> caso o ticket era válido e foi marcado, e
   *         <code>false</code> caso o ticket não fosse válido.
   */
  public boolean check(int id) {
    if (id < this.base) {
      return false;
    }
    else if (id == this.base) {
      discardBase();
      return true;
    }
    else {
      int shift = id - this.base - 1;
      if (shift < size) {
        int idx = (this.index + shift) % size;
        if (FLAG(idx)) {
          return false;
        }
        SET(idx);
        return true;
      }
      else {
        int extra = shift - size;
        if (extra < size) {
          for (int i = 0; i < extra; i++) {
            CLEAR(this.index);
            this.index = (this.index + 1) % size;
          }
        }
        else {
          this.bits = 0;
          this.index = 0;
        }
        this.base += extra;
        discardBase();
        return check(id);
      }
    }
  }
}
