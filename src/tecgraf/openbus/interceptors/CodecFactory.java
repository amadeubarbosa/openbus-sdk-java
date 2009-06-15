/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.UserException;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.Encoding;
import org.omg.PortableInterceptor.ORBInitInfo;

/**
 * Fábrica de codificadores de credenciais.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class CodecFactory {
  /**
   * O nome da referência da fábrica de codificadores.
   */
  private static final String CODEC_FACTORY_ID = "CodecFactory";

  /**
   * Cria um codificador de credenciais.
   * 
   * @param info As informações sobre o ORB.
   * 
   * @return Um codificador de credenciais.
   * 
   * @throws UserException Caso ocorra algum erro na criação do codificador
   */
  public static Codec createCodec(ORBInitInfo info) throws UserException {
    org.omg.IOP.CodecFactory codecFactory = CodecFactoryHelper.narrow(info
      .resolve_initial_references(CODEC_FACTORY_ID));
    Encoding encoding = new Encoding((short) 0, (byte) 1, (byte) 2);
    return codecFactory.create_codec(encoding);
  }
}
