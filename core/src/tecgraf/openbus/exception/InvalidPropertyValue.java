package tecgraf.openbus.exception;

/**
 * Exceção de configuração de propriedade com valor inválido.
 * 
 * @author Tecgraf
 */
public final class InvalidPropertyValue extends OpenBusException {

  /** Nome da propriedade */
  private String prop;
  /** Valor inválido atribuído */
  private String value;

  /**
   * Construtor.
   * 
   * @param prop nome da propriedade
   * @param value valor inválido atribuído.
   */
  public InvalidPropertyValue(String prop, String value) {
    super(String
      .format("Valor da propriedade '%s' é inválido: %s", prop, value));
    this.prop = prop;
    this.value = value;
  }

  /**
   * Construtor.
   * 
   * @param prop nome da propriedade
   * @param value valor inválido atribuído.
   * @param ex exceção associada
   */
  public InvalidPropertyValue(String prop, String value, Throwable ex) {
    super(String
      .format("Valor da propriedade '%s' é inválido: %s", prop, value), ex);
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
