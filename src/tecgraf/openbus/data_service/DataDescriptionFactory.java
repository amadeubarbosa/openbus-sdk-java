/*
 * $Id$
 */
package tecgraf.openbus.data_service;

import java.io.Serializable;

import org.omg.CORBA.portable.ValueFactory;
import org.omg.CORBA_2_3.portable.InputStream;

/**
 * Fábrica para o valuetype {@link DataDescription}.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class DataDescriptionFactory implements ValueFactory {
  /**
   * {@inheritDoc}
   */
  @Override
  public Serializable read_value(InputStream is) {
    return is.read_value(new DataDescriptionImpl());
  }

}
