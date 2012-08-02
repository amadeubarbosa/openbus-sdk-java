package tecgraf.openbus.core;

import java.util.Properties;

import tecgraf.openbus.exception.InvalidPropertyValue;

/**
 * Enumeração que define todas as possíveis propriedades do domínio da
 * biblioteca de acesso, com os seus valores padrões e um local único para
 * recuperar e verificar validade do valor atribuído às propriedades
 * 
 * @author Tecgraf
 */
enum Property {

  /**
   * Chave da propriedade que define se o suporte legado deve ser habilitado ou
   * não. Os valores possíveis são: <code>true</code> e <code>false</code>, onde
   * o padrão é <code>true</code>
   */
  LEGACY_DISABLE("legacy.disable", "false"),
  /**
   * Chave da propriedade que define como o campo delegate do suporte legado
   * deve ser construído a partir de uma credencial 2.0. Os valores possíveis
   * são: "originator" e "caller", onde o padrão é "caller".
   */
  LEGACY_DELEGATE("legacy.delegate", "caller");

  /** Nome da propriedade */
  private final String key;
  /** Valor padrão da propriedade */
  private final String defaultValue;

  /**
   * Construtor.
   * 
   * @param key nome da propriedade.
   * @param value valor padrão.
   */
  private Property(String key, String value) {
    this.key = key;
    this.defaultValue = value;
  }

  /**
   * Recupera a propriedade na lista de propriedades passada. Caso a mesma não
   * esteja especificada na lista, retorna-se o seu valor padrão.
   * 
   * @param props a lista de propriedades.
   * @return o valor da propriedade.
   * @throws InvalidPropertyValue
   */
  String getProperty(Properties props) throws InvalidPropertyValue {
    String value = props.getProperty(this.key, this.defaultValue);
    value = value.toLowerCase();
    switch (this) {
      case LEGACY_DISABLE:
        if (value.equals("true") || value.equals("false")) {
          return value;
        }
        break;

      case LEGACY_DELEGATE:
        if (value.equals("caller") || value.equals("originator")) {
          return value;
        }
        break;

      default:
        return null;
    }
    throw new InvalidPropertyValue(this.key, value);
  }
}
