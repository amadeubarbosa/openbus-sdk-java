package tecgraf.openbus.exception;

/**
 * Exce��o de configura��o de propriedade com valor inv�lido.
 * 
 * @author Tecgraf
 */
public final class InvalidPropertyValue extends OpenBusException {

  /** Nome da propriedade */
  private final String prop;
  /** Valor inv�lido atribu�do */
  private final String value;

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

  /**
   * Construtor.
   * 
   * @param prop nome da propriedade
   * @param value valor inv�lido atribu�do.
   * @param e exce��o associada.
   */
  public InvalidPropertyValue(String prop, String value, Throwable e) {
    super(String
      .format("Valor da propriedade '%s' � inv�lido: %s", prop, value), e);
    this.prop = prop;
    this.value = value;
  }

  /**
   * Recupera o nome da propriedade.
   * 
   * @return o nome da propriedade.
   */
  public String getProperty() {
    return prop;
  }

  /**
   * Recupera o valor da propriedade.
   * 
   * @return o valor da propriedade.
   */
  public String getValue() {
    return value;
  }
}
