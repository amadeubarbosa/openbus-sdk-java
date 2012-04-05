package tecgraf.openbus.util;

/**
 * Hist�rico de tickets
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
   * �ndice corrente no mapa de bits
   */
  private int index;
  /**
   * tamanho padr�o do hist�rico.
   */
  private static final int DEFAULT_SIZE = 32;
  /**
   * o tamanho hist�rico.
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
   * @param size tamanho do hist�rico.
   */
  public TicketsHistory(int size) {
    this.base = 0;
    this.bits = 0;
    this.index = 0;
    this.size = size;
  }

  /**
   * Verifica se a posi��o esta marcada.
   * 
   * @param index o �ndice verificado.
   * @return se posi��o esta marcada ou n�o.
   */
  private boolean FLAG(int index) {
    return (this.bits & (1 << index)) != 0;
  }

  /**
   * Marca a posi��o.
   * 
   * @param index a posi��o.
   */
  private void SET(int index) {
    this.bits |= (1 << index);
  }

  /**
   * Limpa a posi��o.
   * 
   * @param index a posi��o.
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
   * Verifica se o ticket � v�lido e marca com utilizado caso seja v�lido.
   * 
   * @param id o ticket a ser utilizado.
   * @return <code>true</code> caso o ticket era v�lido e foi marcado, e
   *         <code>false</code> caso o ticket n�o fosse v�lido.
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
