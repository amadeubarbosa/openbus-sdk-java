package tecgraf.openbus.core;

import java.util.Properties;

/**
 * Define as possíveis propriedades do domínio da biblioteca de acesso, com
 * os seus valores padrão.
 * 
 * @author Tecgraf
 */
public enum OpenBusProperty {

  /**
   * Define se o suporte legado deve ser desabilitado. Os valores possíveis
   * são: {@code true} e {@code false}. O padrão é {@code false}.
   */
  LEGACY_DISABLE("legacy.disable", "false"),
  /**
   * Caminho para arquivo de chave privada a ser utilizado pela conexão para
   * realizar as chamadas do protocolo OpenBus. A chave deve ser uma chave
   * privada RSA de 2048 bits (256 bytes). Quando essa propriedade não é
   * fornecida, uma chave de acesso é gerada automaticamente.
   */
  ACCESS_KEY("access.key", null),
  /**
   * Caminho para arquivo de certificado a ser utilizado pela conexão para
   * acessar e verificar a identidade de um barramento OpenBus. O
   * certificado deve ser do padrão X509. Quando essa propriedade não é
   * fornecida, o certificado é obtido do barramento automaticamente.
   */
  BUS_CERTIFICATE("bus.certificate", null),
  /**
   * Tamanho das caches utilizadas pela conexão.
   */
  CACHE_SIZE("cache.size", "30"),
  /**
   * Número de threads a serem utilizadas para chamadas assíncronas feitas
   * pela biblioteca, como as de registro ou manutenção de recursos - ofertas
   * e observadores.
   */
  THREAD_NUMBER("thread.number", "10"),
  /**
   * Intervalo de tempo entre tentativas de chamadas remotas para a
   * manutenção de recursos no barramento, como ofertas e observadores.
   */
  TIME_INTERVAL("time.interval", "1000"),
  /**
   * Unidade de tempo de {@link #TIME_INTERVAL}. Utilize "ns" para nanosegundos,
   * "ms" para milisegundos, "s" para segundos, "m" para minutos, "h" para
   * horas e "d" para dias.
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
  OpenBusProperty(String key, String value) {
    this.key = key;
    this.defaultValue = value;
  }

  /**
   * Fornece o nome da chave associada a esta propriedade.
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
      case BUS_CERTIFICATE:
        return props.getProperty(this.key);
      case THREAD_NUMBER:
        return props.getProperty(this.key, this.defaultValue);
      case TIME_INTERVAL:
        return props.getProperty(this.key, this.defaultValue);
      case TIME_UNIT:
        return props.getProperty(this.key, this.defaultValue);
      default:
        return null;
    }
  }
}
