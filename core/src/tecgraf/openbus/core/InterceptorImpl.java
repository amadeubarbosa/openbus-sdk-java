package tecgraf.openbus.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.Interceptor;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.security.Cryptography;

/**
 * Parte comum dos interceptadores cliente e servidor.
 * 
 * @author Tecgraf
 */
abstract class InterceptorImpl extends LocalObject implements Interceptor {
  /** Número de versão Major */
  protected static final byte BUS_MAJOR_VERSION = 2;
  /** Número de versão Minor */
  protected static final byte BUS_MINOR_VERSION = 0;

  /** Tamanho do hash */
  protected static final int HASH_VALUE_SIZE = 32;
  /** Hash nulo. */
  protected static final byte[] NULL_HASH_VALUE = new byte[HASH_VALUE_SIZE];
  /** Tamanho do bloco criptografado */
  protected static final int ENCRYPTED_BLOCK_SIZE = 256;
  /** Bloco nulo criptografado. */
  protected static final byte[] NULL_ENCRYPTED_BLOCK =
    new byte[ENCRYPTED_BLOCK_SIZE];
  /** Cadeia nula assinada. */
  protected static final SignedCallChain NULL_SIGNED_CALL_CHAIN =
    new SignedCallChain(NULL_ENCRYPTED_BLOCK, new byte[0]);
  /** Bloco nulo do suporte legado. */
  protected static final byte[] LEGACY_ENCRYPTED_BLOCK =
    new byte[ENCRYPTED_BLOCK_SIZE];
  /** Hash do suporte legado. */
  protected static final byte[] LEGACY_HASH = new byte[HASH_VALUE_SIZE];

  /** Nome */
  private String name;
  /** Mediador */
  private ORBMediator mediator;

  /**
   * Construtor.
   * 
   * @param name nome.
   * @param mediator Mediador.
   */
  protected InterceptorImpl(String name, ORBMediator mediator) {
    this.name = name;
    this.mediator = mediator;
    Arrays.fill(LEGACY_ENCRYPTED_BLOCK, (byte) 0xff);
    Arrays.fill(LEGACY_HASH, (byte) 0xff);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String name() {
    return this.name;
  }

  /**
   * Recupera o mediador do interceptador.
   * 
   * @return o mediador.
   */
  protected final ORBMediator getMediator() {
    return this.mediator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
  }

  /**
   * Calcula o hash da credencial.
   * 
   * @param ri o request da chamada.
   * @param secret o segredo.
   * @param ticket o ticket utilizado.
   * @return o hash
   */
  protected byte[] generateCredentialDataHash(RequestInfo ri, byte[] secret,
    int ticket) {
    try {
      Cryptography crypto = Cryptography.getInstance();

      MessageDigest hashAlgorithm = crypto.getHashAlgorithm();
      synchronized (hashAlgorithm) {
        hashAlgorithm.update(BUS_MAJOR_VERSION);
        hashAlgorithm.update(BUS_MINOR_VERSION);
        hashAlgorithm.update(secret);

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

  /**
   * Obtém a {@link CallChain} de uma {@link SignedCallChain}
   * 
   * @param chain a cadeia assinada
   * @param logger instância de logger.
   * @return a cadeia associada.
   */
  protected CallChain unmarshallSignedChain(SignedCallChain chain, Logger logger) {
    try {
      Any any =
        this.getMediator().getCodec().decode_value(chain.encoded,
          CallChainHelper.type());
      CallChain callChain = CallChainHelper.extract(any);
      return callChain;
    }
    catch (FormatMismatch e) {
      String message = "Falha inesperada ao decodificar a cadeia";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    catch (TypeMismatch e) {
      String message = "Falha inesperada ao decodificar a cadeia";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

}
