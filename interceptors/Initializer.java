/*
 * $Id$
 */
package openbus.common.interceptors;

import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.Encoding;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName;

/**
 * Essa classe é responsável pelo procedimento de inicialização associado à
 * inicialização do ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
abstract class Initializer extends LocalObject implements ORBInitializer {

  /**
   * Cria um codificador/decodificador de credenciais.
   * 
   * @param info Informações de inicialização do ORB.
   * 
   * @return O codificador/decodificador de credencias.
   * 
   * @throws InvalidName
   * @throws UnknownEncoding
   */
  protected static final Codec createCodec(ORBInitInfo info)
    throws InvalidName, UnknownEncoding {
    CodecFactory codecFactory = CodecFactoryHelper.narrow(info
      .resolve_initial_references("CodecFactory"));
    Encoding encoding = new Encoding((short) 0, (byte) 1, (byte) 2);
    return codecFactory.create_codec(encoding);
  }

  /**
   * {@inheritDoc}
   */
  public void pre_init(ORBInitInfo info) {
  }
}
