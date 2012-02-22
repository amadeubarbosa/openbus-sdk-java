package tecgraf.openbus.defaultimpl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.NO_PERMISSIONHelper;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.CryptographyException;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.v2_00.credential.CredentialContextId;
import tecgraf.openbus.core.v2_00.credential.CredentialData;
import tecgraf.openbus.core.v2_00.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_00.credential.CredentialReset;
import tecgraf.openbus.core.v2_00.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChain;

public final class ClientRequestInterceptorImpl extends InterceptorImpl
  implements ClientRequestInterceptor {
  private static final Logger logger = Logger
    .getLogger(ClientRequestInterceptorImpl.class.getName());
  private Map<EffectiveProfile, String> entities;
  private Map<String, CredentialReset> resets;
  private Map<String, CredentialSession> sessions;

  ClientRequestInterceptorImpl(String name, ORBMediator mediator) {
    super(name, mediator);
    this.entities = new HashMap<EffectiveProfile, String>();
    this.resets = new HashMap<String, CredentialReset>();
    this.sessions = new HashMap<String, CredentialSession>();
  }

  @Override
  public void send_request(ClientRequestInfo ri) {
    String operation = ri.operation();
    logger.finest(String.format("A operação %s será requisitada", operation));
    BusORB orb = this.getMediator().getORB();
    if (orb.isCurrentThreadIgnored()) {
      logger
        .finest(String
          .format(
            "A operação %s não terá uma credencial, pois a thread atual está ignorada",
            operation));
      return;
    }
    CredentialData credential = this.generateCredentialData(ri);
    Any any = orb.getORB().create_any();
    CredentialDataHelper.insert(any, credential);
    byte[] encodedCredential;
    try {
      encodedCredential = this.getMediator().getCodec().encode_value(any);
    }
    catch (InvalidTypeForEncoding e) {
      String message = "Falha inesperada ao codificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    ServiceContext requestServiceContext =
      new ServiceContext(CredentialContextId.value, encodedCredential);
    ri.add_request_service_context(requestServiceContext, false);
  }

  private CredentialData generateCredentialData(ClientRequestInfo ri) {
    BusORB orb = this.getMediator().getORB();
    Connection currentConnection = orb.getCurrentConnection();

    String busId = currentConnection.getBus().getId();
    String loginId = currentConnection.getLogin().id;

    EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
    if (this.entities.containsKey(ep)) {
      String callee = this.entities.get(ep);
      CredentialSession session =
        this.getCredentialSession(callee, currentConnection);
      if (session != null) {
        session.generateNextTicket();
        byte[] credentialDataHash =
          this.generateCredentialDataHash(ri, session);
        return new CredentialData(busId, loginId, session.getSession(), session
          .getTicket(), credentialDataHash, session.getDefaultCallChain());
      }
    }

    return new CredentialData(busId, loginId, 0, 0,
      Cryptography.NULL_HASH_VALUE, Cryptography.NULL_SIGNED_CALL_CHAIN);
  }

  private CredentialSession getCredentialSession(String callee,
    Connection currentConnection) {
    CredentialSession session = this.sessions.get(callee);
    if (session != null) {
      return session;
    }
    CredentialReset reset = this.resets.remove(callee);
    if (reset == null) {
      return null;
    }

    Cryptography crypto = Cryptography.getInstance();
    byte[] secret;
    try {
      secret =
        crypto.decrypt(reset.challenge, currentConnection.getPrivateKey());
    }
    catch (CryptographyException e) {
      String message =
        String.format("Falha inesperada ao decifrar o segredo da entidade %s",
          callee);
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

    SignedCallChain callChain;
    if (callee.equals(currentConnection.getBus().getId())) {
      callChain = Cryptography.NULL_SIGNED_CALL_CHAIN;
    }
    else {
      try {
        callChain = currentConnection.getAccessControl().signChainFor(callee);
      }
      catch (ServiceFailure e) {
        String message =
          String
            .format(
              "Falha inesperada ao assinar uma cadeia de chamadas para a entidade %s",
              callee);
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
    }
    session = new CredentialSession(reset.session, secret, callChain);
    this.sessions.put(callee, session);
    return session;
  }

  private byte[] generateCredentialDataHash(ClientRequestInfo ri,
    CredentialSession credentialSession) {
    Cryptography crypto = Cryptography.getInstance();

    MessageDigest hashAlgorithm;
    try {
      hashAlgorithm = crypto.getHashAlgorithm();
    }
    catch (CryptographyException e) {
      String message = "Falha inesperada ao obter o algoritmo de hash";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

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

    byte[] operationBytes = ri.operation().getBytes(OpenBus.CHARSET);
    hashAlgorithm.update(operationBytes);

    return hashAlgorithm.digest();
  }

  @Override
  public void send_poll(ClientRequestInfo ri) {
  }

  @Override
  public void receive_reply(ClientRequestInfo ri) {
  }

  @Override
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    logger.finest("Uma exceção foi recebida");
    Cryptography crypto = Cryptography.getInstance();

    if (!ri.received_exception_id().equals(NO_PERMISSIONHelper.id())) {
      logger.fine("A exceção recebida não é do tipo NO_PERMISSION");
      return;
    }
    logger.fine("A exceção recebida é do tipo NO_PERMISSION");

    Any exceptionAny = ri.received_exception();
    NO_PERMISSION exception = NO_PERMISSIONHelper.extract(exceptionAny);
    if (!exception.completed.equals(CompletionStatus.COMPLETED_NO)) {
      return;
    }
    logger
      .fine("A exceção indica que a operação solicitada não foi completada");

    if (exception.minor == InvalidCredentialCode.value) {
      logger.fine("Obtendo o CredentialReset");
      EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
      ServiceContext context =
        ri.get_reply_service_context(CredentialContextId.value);
      Any credentialResetAny = null;
      try {
        credentialResetAny =
          this.getMediator().getCodec().decode_value(context.context_data,
            CredentialResetHelper.type());
      }
      catch (FormatMismatch e) {
        String message = "Falha inesperada ao obter o CredentialReset";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
      catch (TypeMismatch e) {
        String message = "Falha inesperada ao obter o CredentialReset";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }

      CredentialReset reset = CredentialResetHelper.extract(credentialResetAny);
      String callee = reset.login;
      this.entities.put(ep, callee);
      this.resets.put(callee, reset);
      logger.fine("Solicitando que a chamada seja refeita.");
      throw new ForwardRequest(ri.target());
    }
  }

  @Override
  public void receive_other(ClientRequestInfo ri) {
  }
}
