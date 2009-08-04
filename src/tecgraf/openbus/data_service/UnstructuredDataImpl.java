/*
 * $Id$
 */
package tecgraf.openbus.data_service;

/**
 * Representa a visão não-estruturada de um dado.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class UnstructuredDataImpl extends UnstructuredData {
  /**
   * Construtor padrão, necessário para o mecanismo de serialização de CORBA.
   */
  UnstructuredDataImpl() {
  }

  /**
   * Cria um UnstructuredData.
   * 
   * @param key A chave do dado representado por esta visão.
   * @param host A máquina onde o dado está disponível.
   * @param port A porta onde o dado está disponível.
   * @param accessKey A chave de acesso para controle de permissão.
   * @param writable Indica se o dado pode ser escrito ou se pode ser apenas
   *        lido.
   */
  public UnstructuredDataImpl(byte[] key, String host, int port,
    byte[] accessKey, boolean writable) {
    this.fKey = key;
    this.fHost = host;
    this.fPort = port;
    this.fAccessKey = accessKey;
    this.fWritable = writable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] getKey() {
    return this.fKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getInterfaceName() {
    return UnstructuredDataHelper.id();
  }
}
