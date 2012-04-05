package tecgraf.openbus.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;

import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.Interceptor;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.util.Cryptography;

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

  /** Nome */
  private String name;
  /** Mediador */
  private ORBMediator mediator;

  /** Tamanho das caches dos interceptadores */
  protected final int CACHE_SIZE = 30;

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

}
