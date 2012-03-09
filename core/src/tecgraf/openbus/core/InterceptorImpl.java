package tecgraf.openbus.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;

import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.Interceptor;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.core.interceptor.CredentialSession;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.util.Cryptography;

abstract class InterceptorImpl extends LocalObject implements Interceptor {
  protected static final byte BUS_MAJOR_VERSION = 2;
  protected static final byte BUS_MINOR_VERSION = 0;

  private String name;
  private ORBMediator mediator;

  /** Tamanho das caches dos interceptadores */
  protected final int CACHE_SIZE = 30;

  protected InterceptorImpl(String name, ORBMediator mediator) {
    this.name = name;
    this.mediator = mediator;
  }

  @Override
  public String name() {
    return this.name;
  }

  protected final ORBMediator getMediator() {
    return this.mediator;
  }

  @Override
  public void destroy() {
  }

  /**
   * Calcula o hash da credencial.
   * 
   * @param ri o request da chamada.
   * @param credentialSession a sessão.
   * @return o hash
   */
  protected byte[] generateCredentialDataHash(RequestInfo ri,
    CredentialSession credentialSession) {
    try {
      Cryptography crypto = Cryptography.getInstance();

      MessageDigest hashAlgorithm = crypto.getHashAlgorithm();
      hashAlgorithm.update(BUS_MAJOR_VERSION);
      hashAlgorithm.update(BUS_MINOR_VERSION);
      hashAlgorithm.update(credentialSession.getSecret());

      ByteBuffer ticketBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
      ticketBuffer.order(ByteOrder.LITTLE_ENDIAN);
      ticketBuffer.putInt(credentialSession.getTicket());
      ticketBuffer.flip();
      hashAlgorithm.update(ticketBuffer);

      ByteBuffer requestIdBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
      requestIdBuffer.order(ByteOrder.LITTLE_ENDIAN);
      requestIdBuffer.putInt(ri.request_id());
      requestIdBuffer.flip();
      hashAlgorithm.update(requestIdBuffer);

      byte[] operationBytes = ri.operation().getBytes(Cryptography.CHARSET);
      hashAlgorithm.update(operationBytes);

      return hashAlgorithm.digest();
    }
    catch (CryptographyException e) {
      String message = "Falha inesperada ao obter o algoritmo de hash";
      throw new INTERNAL(message);
    }
  }
}
