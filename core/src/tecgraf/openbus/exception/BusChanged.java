package tecgraf.openbus.exception;

/**
 * Exce��o indicativa de que a conex�o tentou relogar em um barramento
 * diferente.
 * 
 * @author Tecgraf
 */
public class BusChanged extends OpenBusException {

  /** Identificador do barramento */
  private String busid;

  /**
   * Construtor.
   * 
   * @param busid identificador do barramento que tentou se conectar
   */
  public BusChanged(String busid) {
    super(String
      .format(
        "Barramento inv�lido! Identificador do barramento mudou para '%s'",
        busid));
    this.busid = busid;
  }

  /**
   * Recupera o identificador do barramento que tentou se conectar.
   * 
   * @return o identificador do barramento.
   */
  public String getBusid() {
    return busid;
  }

}
