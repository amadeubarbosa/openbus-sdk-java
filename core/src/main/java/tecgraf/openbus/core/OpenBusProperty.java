package tecgraf.openbus.core;

import java.util.Properties;

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
   * não. Os valores possíveis sÃo: <code>true</code> e <code>false</code>, onde
   * o padrão é <code>false</code>
   */
  LEGACY_DISABLE("legacy.disable", "false"),
  /**
   * Chave da propriedade que define arquivo da chave privada a ser utilizada
   * pela conexão.
   */
  ACCESS_KEY("access.key", null),
  /**
   * Chave da propriedade que define tamanho de cada cache utilizada pela
   * conexão.
   */
  CACHE_SIZE("cache.size", "30"),
  /**
   * Número de threads a serem utilizadas para chamadas assíncronas.
   */
  THREAD_NUM("thread.number", "10"),
  /**
   * Intervalo de tempo entre tentativas de chamadas remotas.
   */
  TIME_INTERVAL("time.interval", "1000"),
  /**
   * Unidade de tempo do intervalo. Utilize "ns" para nanosegundos, "ms" para
   * milisegundos, "s" para segundos e "m" para minutos.
   */
  TIME_UNIT("time.unit", "ms");

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
   */
  String getProperty(Properties props) {
    switch (this) {
      case LEGACY_DISABLE:
        return props.getProperty(this.key, this.defaultValue);

      case ACCESS_KEY:
        return props.getProperty(this.key);

      case CACHE_SIZE:
        return props.getProperty(this.key, this.defaultValue);

      default:
        return null;
    }
  }
}
