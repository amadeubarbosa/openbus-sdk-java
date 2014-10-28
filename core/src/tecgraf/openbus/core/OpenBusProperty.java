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
enum OpenBusProperty {

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
  LEGACY_DELEGATE("legacy.delegate", "caller"),
  /**
   * Chave da propriedade que define arquivo da chave privada a ser utilizada
   * pela conexão.
   */
  ACCESS_KEY("access.key", null),
  /**
   * Chave da propriedade que define tamanho de cada cache utilizada pela
   * conexão.
   */
  CACHE_SIZE("cache.size", "30");

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
  private OpenBusProperty(String key, String value) {
    this.key = key;
    this.defaultValue = value;
  }

  /**
   * Recupera o nome da chave associado a esta propriedade.
   * 
   * @return o nome da chave.
   */
  public String getKey() {
    return key;
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
    switch (this) {
      case LEGACY_DISABLE:
        value = value.toLowerCase();
        if (value.equals("true") || value.equals("false")) {
          return value;
        }
        break;

      case LEGACY_DELEGATE:
        value = value.toLowerCase();
        if (value.equals("caller") || value.equals("originator")) {
          return value;
        }
        break;

      case ACCESS_KEY:
        return value;

      case CACHE_SIZE:
        try {
          int size = Integer.parseInt(value);
          if (size > 0) {
            return value;
          }
        }
        catch (NumberFormatException e) {
          throw new InvalidPropertyValue(this.key, value, e);
        }
        break;

      default:
        return null;
    }
    throw new InvalidPropertyValue(this.key, value);
  }
}
