/*
 * $Id$
 */
package tecgraf.openbus.data_service;

import java.util.List;
import java.util.Set;

/**
 * Implementa��o do tipo DataDescription.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class DataDescriptionImpl extends DataDescription {
  /**
   * Construtor padr�o, necess�rio para o mecanismo de serializa��o de CORBA.
   */
  DataDescriptionImpl() {
  }

  /**
   * Cria um DataDescription.
   * 
   * @param key A chave do dado.
   * @param name O nome simb�lico do dado.
   * @param views As vis�es oferecidas pelo dado.
   */
  public DataDescriptionImpl(byte[] key, String name, Set<String> views) {
    this.fKey = key;
    this.fName = name;
    this.fViews = views.toArray(new String[0]);
  }

  /**
   * Cria um DataDescription.
   * 
   * @param key A chave do dado.
   * @param name O nome simb�lico do dado.
   * @param views As vis�es oferecidas pelo dado.
   * @param metadata Metadados do dado.
   */
  public DataDescriptionImpl(byte[] key, String name, Set<String> views,
    List<Metadata> metadata) {
    this(key, name, views);
    this.fMetadata = metadata.toArray(new Metadata[0]);
  }
}
