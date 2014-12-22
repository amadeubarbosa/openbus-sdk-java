package tecgraf.openbus.exception;

/**
 * Exceção indicando tentativa de uso de recurso pertencente a outro barramento.
 * 
 * @author Tecgraf
 */
public final class WrongBus extends OpenBusException {

  /**
   * Construtor.
   */
  public WrongBus() {
    super("Tentativa de uso de recurso pertencente a outro barramento.");
  }
}
