package tecgraf.openbus.exception;

/**
 * Exce��o de configura��o de propriedade com valor inv�lido.
 * 
 * @author Tecgraf
 */
public final class InvalidPropertyValue extends OpenBusException {

  /** Nome da propriedade */
  private String prop;
  /** Valor inv�lido atribu�do */
  private String value;

  /**
   * Construtor.
   * 
   * @param prop nome da propriedade
   * @param value valor inv�lido atribu�do.
   */
  public InvalidPropertyValue(String prop, String value) {
    super(String
      .format("Valor da propriedade '%s' � inv�lido: %s", prop, value));
    this.prop = prop;
    this.value = value;
  }

}
