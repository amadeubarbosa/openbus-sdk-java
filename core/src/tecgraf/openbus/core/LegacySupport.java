/**
 * $Id$
 */
package tecgraf.openbus.core;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper;

/**
 * Classe utilitária para realização do suporete legado
 *
 * @author Tecgraf/PUC-Rio
 */
public class LegacySupport {

  /** Bloco nulo do suporte legado. */
  protected static final byte[] LEGACY_ENCRYPTED_BLOCK =
    new byte[InterceptorImpl.ENCRYPTED_BLOCK_SIZE];
  /** Hash do suporte legado. */
  protected static final byte[] LEGACY_HASH =
    new byte[InterceptorImpl.HASH_VALUE_SIZE];

  /**
   * Constrói uma cadeia assinada legada à partir das informações da cadeia
   * legada.
   * 
   * @param legacy a cadeia legada
   * @param orb o orb
   * @param codec o codec
   * @return A cadeia assinada legada
   * @throws InvalidTypeForEncoding
   */
  static protected SignedCallChain buildLegacySignedChain(CallChain legacy,
    ORB orb, Codec codec) throws InvalidTypeForEncoding {
    Any anyCallChain = orb.create_any();
    CallChainHelper.insert(anyCallChain, legacy);
    byte[] encoded = codec.encode_value(anyCallChain);
    return new SignedCallChain(LEGACY_ENCRYPTED_BLOCK, encoded);
  }

}
