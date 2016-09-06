package tecgraf.openbus.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;

import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.Interceptor;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_1.EncryptedBlockSize;
import tecgraf.openbus.core.v2_1.HashValueSize;
import tecgraf.openbus.core.v2_1.MajorVersion;
import tecgraf.openbus.core.v2_1.MinorVersion;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.security.Cryptography;

/**
 * Parte comum dos interceptadores cliente e servidor.
 * 
 * @author Tecgraf
 */
abstract class InterceptorImpl extends LocalObject implements Interceptor {
  /** Número de versão Major */
  protected static final byte BUS_MAJOR_VERSION = MajorVersion.value;
  /** Número de versão Minor */
  protected static final byte BUS_MINOR_VERSION = MinorVersion.value;

  /** Tamanho do hash */
  protected static final int HASH_VALUE_SIZE = HashValueSize.value;
  /** Hash nulo. */
  protected static final byte[] NULL_HASH_VALUE = new byte[HASH_VALUE_SIZE];
  /** Tamanho do bloco criptografado */
  protected static final int ENCRYPTED_BLOCK_SIZE = EncryptedBlockSize.value;
  /** Bloco nulo criptografado. */
  protected static final byte[] NULL_ENCRYPTED_BLOCK =
    new byte[ENCRYPTED_BLOCK_SIZE];
  /** Cadeia nula assinada. */
  protected static final SignedData NULL_SIGNED_CALL_CHAIN = new SignedData(
    NULL_ENCRYPTED_BLOCK, new byte[0]);

  /** Número de versão Legada Major */
  protected static final byte LEGACY_MAJOR_VERSION =
    tecgraf.openbus.core.v2_0.MajorVersion.value;
  /** Número de versão Legada Minor */
  protected static final byte LEGACY_MINOR_VERSION =
    tecgraf.openbus.core.v2_0.MinorVersion.value;
  /** Cadeia nula legada assinada. */
  protected static final SignedCallChain NULL_SIGNED_LEGACY_CALL_CHAIN =
    new SignedCallChain(NULL_ENCRYPTED_BLOCK, new byte[0]);

  /** Nome */
  private final String name;
  /** Mediador */
  private final ORBMediator mediator;

  /**
   * Construtor.
   * 
   * @param name nome.
   * @param mediator Mediador.
   */
  protected InterceptorImpl(String name, ORBMediator mediator) {
    this.name = name;
    this.mediator = mediator;
  }

  @Override
  public String name() {
    return this.name;
  }

  /**
   * Recupera o mediador do interceptador.
   * 
   * @return o mediador.
   */
  protected final ORBMediator mediator() {
    return mediator;
  }

  /**
   * Recupera o ORB associado ao interceptador
   * 
   * @return o ORB
   */
  protected final ORB orb() {
    return mediator.getORB();
  }

  /**
   * Recupera o codec associado ao interceptador
   * 
   * @return o codec
   */
  protected final Codec codec() {
    return mediator.getCodec();
  }

  /**
   * Recupera o contexto associado ao interceptador.
   * 
   * @return o contexto
   */
  protected final OpenBusContextImpl context() {
    return mediator.getContext();
  }

  @Override
  public void destroy() {
  }

  /**
   * Calcula o hash da credencial.
   * 
   * @param ri o request da chamada.
   * @param secret o segredo.
   * @param ticket o ticket utilizado.
   * @param legacy indicador se em modo legado.
   * @return o hash
   */
  protected byte[] generateCredentialDataHash(RequestInfo ri, byte[] secret,
    int ticket, boolean legacy) {
    try {
      Cryptography crypto = Cryptography.getInstance();

      MessageDigest hashAlgorithm = crypto.getHashAlgorithm();
      synchronized (hashAlgorithm) {
        if (!legacy) {
          hashAlgorithm.update(BUS_MAJOR_VERSION);
          hashAlgorithm.update(BUS_MINOR_VERSION);
          hashAlgorithm.update(secret);
        }
        else {
          hashAlgorithm.update(LEGACY_MAJOR_VERSION);
          hashAlgorithm.update(LEGACY_MINOR_VERSION);
          hashAlgorithm.update(secret);
        }

        ByteBuffer ticketBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
        ticketBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ticketBuffer.putInt(ticket);
        ticketBuffer.flip();
        hashAlgorithm.update(ticketBuffer);

        byte[] operationBytes = ri.operation().getBytes(Cryptography.CHARSET);
        hashAlgorithm.update(operationBytes);

        return hashAlgorithm.digest();
      }
    }
    catch (CryptographyException e) {
      String message = "Falha inesperada ao calcular o hash da credencial";
      throw new INTERNAL(message);
    }

  }

}
