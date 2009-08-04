/*
 * $Id$
 */
package tecgraf.openbus.data_service;

/**
 * Representa a vis�o n�o-estruturada de um dado.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class UnstructuredDataImpl extends UnstructuredData {
  /**
   * Construtor padr�o, necess�rio para o mecanismo de serializa��o de CORBA.
   */
  UnstructuredDataImpl() {
  }

  /**
   * Cria um UnstructuredData.
   * 
   * @param key A chave do dado representado por esta vis�o.
   * @param host A m�quina onde o dado est� dispon�vel.
   * @param port A porta onde o dado est� dispon�vel.
   * @param accessKey A chave de acesso para controle de permiss�o.
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
