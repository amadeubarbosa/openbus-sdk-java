package tecgraf.openbus.util;

public class TicketsHistory {

  private int base;
  private int bits;
  private int index;
  private static final int DEFAULT_SIZE = 32;
  private final int size;

  public TicketsHistory() {
    this(DEFAULT_SIZE);
  }

  public TicketsHistory(int size) {
    this.base = 0;
    this.bits = 0;
    this.index = 0;
    this.size = size;
  }

  private boolean FLAG(int index) {
    return (this.bits & (1 << index)) != 0;
  }

  private void SET(int index) {
    this.bits |= (1 << index);
  }

  private int CLEAR(int index) {
    return this.bits &= ~(1 << index);
  }

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
