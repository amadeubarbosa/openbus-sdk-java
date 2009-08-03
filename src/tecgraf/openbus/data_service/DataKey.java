/*
 * $Id$
 */
package tecgraf.openbus.data_service;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import scs.core.ComponentId;

/**
 * Representa a chave unívoca de um dado. A chave unívoca é composta por 5
 * elementos:
 * <ul>
 * <li>O identificador do dado dentro de seu serviço de origem.</li>
 * <li>O nome da interface de seu serviço de origem.</li>
 * <li>O identificador do componente de seu serviço de origem.</li>
 * <li>O nome da faceta do componente de seu serviço de origem.</li>
 * <li>O IOR da faceta do componente de seu serviço de origem.</li>
 * </ul>
 * 
 * Apenas o identificador do dado no seu serviço de origem é obrigatório.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class DataKey {
  /**
   * O tamanho do buffer para conversão da DataKey em byte[].
   */
  private static final int BUFFER_SIZE = 1024;
  /**
   * O nome do conjunto de caracteres utilizado para conversão das string.
   */
  private static final String CHARSET_NAME = "UTF8";
  /**
   * O separador do identificador do componente.
   */
  private static final String COMPONENT_ID_SEPARATOR = ":";
  /**
   * O separador da versão do identificador do componente.
   */
  private static final String COMPONENT_ID_VERSION_SEPARATOR = ".";
  /**
   * A chave real.
   */
  private byte[] key;
  /**
   * O identificador do dado dentro de seu serviço de origem. (Obrigatório)
   */
  private String dataId;
  /**
   * O nome da interface da faceta do serviço de origem do dado. (Obrigatório)
   */
  private String serviceInterfaceName;
  /**
   * O identificador do componente do serviço de origem do dado. (Opcional)
   */
  private ComponentId serviceComponentId;
  /**
   * O nome da faceta do serviço de origem do dado. (Opcional)
   */
  private String serviceFacetName;
  /**
   * O IOR da faceta do serviço de origem do dado. (Opcional)
   */
  private String serviceFacetIOR;

  /**
   * Cria um DataKey a partir da sua representação em bytes.
   * 
   * @param key A representação em bytes.
   * 
   * @throws InvalidDataKey Caso a representação recebida seja inválida.
   */
  public DataKey(byte[] key) throws InvalidDataKey {
    ByteBuffer buffer = ByteBuffer.wrap(key);
    try {
      this.dataId = readString(buffer);
      this.serviceInterfaceName = readString(buffer);
      this.serviceComponentId = generateComponentId(readString(buffer));
      this.serviceFacetName = readString(buffer);
      this.serviceFacetIOR = readString(buffer);
      if (buffer.remaining() != 0) {
        throw new InvalidDataKey(key);
      }
    }
    catch (UnsupportedEncodingException e) {
      throw new InvalidDataKey(key);
    }
    this.key = Arrays.copyOf(key, key.length);
  }

  /**
   * Cria a chave unívoca de um dado.
   * 
   * @param dataId O identificador do dado dentro de seu serviço de origem.
   * 
   * @throws InvalidDataKey Caso não seja possível representar os dados
   *         recebidos como um array de bytes.
   */
  public DataKey(String dataId) throws InvalidDataKey {
    this(dataId, "", null, "", "");
  }

  /**
   * Cria a chave unívoca de um dado.
   * 
   * @param dataId O identificador do dado dentro de seu serviço de origem.
   * @param serviceInterfaceName O nome da faceta do serviço de origem do dado.
   * @param serviceComponentId O identificador do componente de origem do dado.
   * @param serviceFacetName O nome da faceta do serviço de origem do dado.
   * @param serviceFacetIOR O IOR da faceta do serviço de origem do dado.
   * 
   * @throws InvalidDataKey Caso não seja possível representar os dados
   *         recebidos como um array de bytes.
   */
  public DataKey(String dataId, String serviceInterfaceName,
    ComponentId serviceComponentId, String serviceFacetName,
    String serviceFacetIOR) throws InvalidDataKey {
    if (dataId == null) {
      throw new IllegalArgumentException(
        "O identificador real do dado não pode ser nulo.");
    }
    this.dataId = dataId;

    if (serviceInterfaceName == null) {
      this.serviceInterfaceName = "";
    }
    else {
      this.serviceInterfaceName = serviceInterfaceName;
    }

    this.serviceComponentId = serviceComponentId;

    if (serviceFacetName == null) {
      this.serviceFacetName = "";
    }
    else {
      this.serviceFacetName = serviceFacetName;
    }

    if (serviceFacetIOR == null) {
      this.serviceFacetIOR = "";
    }
    else {
      this.serviceFacetIOR = serviceFacetIOR;
    }

    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    try {
      putString(buffer, this.dataId);
      putString(buffer, this.serviceInterfaceName);
      putString(buffer, generateComponentIdString(this.serviceComponentId));
      putString(buffer, this.serviceFacetName);
      putString(buffer, this.serviceFacetIOR);
    }
    catch (UnsupportedEncodingException e) {
      throw new InvalidDataKey();
    }

    buffer.flip();
    this.key = new byte[buffer.limit()];
    buffer.get(this.key);
  }

  /**
   * Lê uma string a partir de um buffer de bytes.
   * 
   * @param byteBuffer O buffer de bytes.
   * 
   * @return A string lida.
   * 
   * @throws UnsupportedEncodingException Caso não seja possível codificar a
   *         string como {@value #CHARSET_NAME};
   */
  private static String readString(ByteBuffer byteBuffer)
    throws UnsupportedEncodingException {
    int valueLength = byteBuffer.getInt();
    byte[] value = new byte[valueLength];
    byteBuffer.get(value);
    return new String(value, CHARSET_NAME);
  }

  /**
   * Insere uma string em um buffer de bytes.
   * 
   * @param byteBuffer O buffer de bytes.
   * @param value A string.
   * 
   * @throws UnsupportedEncodingException Caso a string não esteja codificada
   *         como {@value #CHARSET_NAME}.
   */
  private static void putString(ByteBuffer byteBuffer, String value)
    throws UnsupportedEncodingException {
    byte[] valueBytes = value.getBytes(CHARSET_NAME);
    byteBuffer.putInt(valueBytes.length);
    byteBuffer.put(valueBytes);
  }

  /**
   * Gera um identificador de componente a partir de uma string.
   * 
   * @param componentIdString A string que representa o identificador do
   *        componente.
   * 
   * @return O identificador do componente, ou {@code null}, caso a string
   *         esteja vazia.
   */
  private static ComponentId generateComponentId(String componentIdString) {
    if (componentIdString.equals("")) {
      return null;
    }
    String[] splittedComponentId =
      componentIdString.split(COMPONENT_ID_SEPARATOR);
    String[] splittedVersion =
      splittedComponentId[1].split(COMPONENT_ID_VERSION_SEPARATOR);
    return new ComponentId(splittedComponentId[0], Byte
      .parseByte(splittedVersion[0]), Byte.parseByte(splittedVersion[1]), Byte
      .parseByte(splittedVersion[2]), null);
  }

  /**
   * Gera uma string representando o identificador do componente de origem do
   * dado.
   * 
   * @param componentId O identificador do componente do dado.
   * 
   * @return Uma string representando o identificador do componente de origem do
   *         dado. Caso o identificador do componente seja {@code null}, será
   *         retornada uma string vazia.
   */
  private static String generateComponentIdString(ComponentId componentId) {
    if (componentId == null) {
      return "";
    }
    return componentId.name + COMPONENT_ID_SEPARATOR
      + componentId.major_version + COMPONENT_ID_VERSION_SEPARATOR
      + componentId.minor_version + COMPONENT_ID_VERSION_SEPARATOR
      + componentId.patch_version;
  }

  /**
   * Obtém o identificador do dado dentro de seu serviço de origem.
   * 
   * @return O identificador do dado dentro de seu serviço de origem.
   */
  public String getDataId() {
    return this.dataId;
  }

  /**
   * Obtém o nome da interface da faceta do serviço de origem do dado.
   * 
   * @return O nome da interface da faceta do serviço de origem do dado.
   */
  public String getServiceInterfaceName() {
    return this.serviceInterfaceName;
  }

  /**
   * Obtém o identificador do componente do serviço de origem do dado.
   * 
   * @return O identificador do componente do serviço de origem do dado.
   */
  public ComponentId getServiceComponentId() {
    return this.serviceComponentId;
  }

  /**
   * Obtém o nome da faceta do serviço de origem do dado.
   * 
   * @return O nome da faceta do serviço de origem do dado.
   */
  public String getServiceFacetName() {
    return this.serviceFacetName;
  }

  /**
   * Obtém o IOR da faceta do serviço de origem do dado.
   * 
   * @return O IOR da faceta do serviço de origem do dado.
   */
  public String getServiceFacetIOR() {
    return this.serviceFacetIOR;
  }

  /**
   * Obtém a chave do dado.
   * 
   * @return A chave do dado.
   */
  public byte[] getKey() {
    return this.key;
  }
}
