package tecgraf.openbus.core;

import java.util.Collections;
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

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.interceptor.CredentialSession;
import tecgraf.openbus.core.interceptor.EffectiveProfile;
import tecgraf.openbus.core.v2_00.credential.CredentialContextId;
import tecgraf.openbus.core.v2_00.credential.CredentialData;
import tecgraf.openbus.core.v2_00.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_00.credential.CredentialReset;
import tecgraf.openbus.core.v2_00.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChain;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.LRUCache;

/**
 * Implementação do interceptador cliente.
 * 
 * @author Tecgraf
 */
final class ClientRequestInterceptorImpl extends InterceptorImpl implements
  ClientRequestInterceptor {

  /** Instância de logging. */
  private static final Logger logger = Logger
    .getLogger(ClientRequestInterceptorImpl.class.getName());

  /** Mapa de profile do interceptador para o cliente alvo da chamanha */
  private Map<EffectiveProfile, String> entities;
  /** Mapa de cliente alvo da chamada para estrutura de reset */
  private Map<String, CredentialReset> resets;
  /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
  private Map<String, CredentialSession> sessions;

  /**
   * Construtor.
   * 
   * @param name nome do interceptador
   * @param mediator o mediador do ORB
   */
  ClientRequestInterceptorImpl(String name, ORBMediator mediator) {
    super(name, mediator);
    this.entities =
      Collections.synchronizedMap(new LRUCache<EffectiveProfile, String>(
        CACHE_SIZE));
    this.resets =
      Collections.synchronizedMap(new LRUCache<String, CredentialReset>(
        CACHE_SIZE));
    this.sessions =
      Collections.synchronizedMap(new LRUCache<String, CredentialSession>(
        CACHE_SIZE));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_request(ClientRequestInfo ri) {
    String operation = ri.operation();
    logger.fine(String.format("A operação %s será requisitada", operation));
    BusORBImpl orb = (BusORBImpl) this.getMediator().getORB();
    if (orb.isCurrentThreadIgnored()) {
      logger.fine(String.format("Realizando requisição sem credencial: %s",
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

  /**
   * Gera uma credencial para a chamada.
   * 
   * @param ri Informação do request
   * @return A credencial válida para a sessão, ou uma credencial para forçar o
   *         reset da sessão.
   */
  private CredentialData generateCredentialData(ClientRequestInfo ri) {
    BusORBImpl orb = (BusORBImpl) this.getMediator().getORB();
    ConnectionMultiplexerImpl multiplexer = orb.getConnectionMultiplexer();
    Connection currentConnection = multiplexer.getCurrentConnection();

    String busId = currentConnection.busid();
    String loginId = currentConnection.login().id;

    EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
    if (this.entities.containsKey(ep)) {
      String callee = this.entities.get(ep);
      CredentialSession session =
        this.getCredentialSession(callee, currentConnection);
      if (session != null) {
        logger.fine(String.format("reusando sessão: id = %d ticket = %d",
          session.getSession(), session.getTicket()));
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

  /**
   * Recupera a credencial da sessão. Se a sessão ainda não existe, realiza a
   * negociação para a geração de uma nova sessão.
   * 
   * @param callee o cliente alvo da chamada.
   * @param currentConnection a conexão pela qual a chamada será realizada.
   * @return A credencial da sessão com este cliente.
   */
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
        crypto.decrypt(reset.challenge, ((ConnectionImpl) currentConnection)
          .getPrivateKey());
    }
    catch (CryptographyException e) {
      String message =
        String.format("Falha inesperada ao decifrar o segredo da entidade %s",
          callee);
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

    SignedCallChain callChain;
    if (callee.equals(currentConnection.busid())) {
      CallerChain joinedChain = currentConnection.getJoinedChain();
      if (joinedChain != null) {
        callChain = ((CallerChainImpl) joinedChain).signedCallChain();
      }
      else {
        callChain = Cryptography.NULL_SIGNED_CALL_CHAIN;
      }
    }
    else {
      try {
        callChain =
          ((ConnectionImpl) currentConnection).access().signChainFor(callee);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_poll(ClientRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_reply(ClientRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    logger.finest("Uma exceção foi recebida");
    if (!ri.received_exception_id().equals(NO_PERMISSIONHelper.id())) {
      logger.finest("A exceção recebida não é do tipo NO_PERMISSION");
      return;
    }
    logger.finest("A exceção recebida é do tipo NO_PERMISSION");

    Any exceptionAny = ri.received_exception();
    NO_PERMISSION exception = NO_PERMISSIONHelper.extract(exceptionAny);
    if (!exception.completed.equals(CompletionStatus.COMPLETED_NO)) {
      return;
    }
    logger
      .finest("A exceção indica que a operação solicitada não foi completada");

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
    else if (exception.minor == InvalidLoginCode.value) {
      logger.fine(String.format(
        "Recebeu uma exceção InvalidLogin. operação: %s", ri.operation()));
      BusORBImpl orb = (BusORBImpl) this.getMediator().getORB();
      ConnectionMultiplexerImpl multiplexer = orb.getConnectionMultiplexer();
      Connection conn = multiplexer.getCurrentConnection();
      InvalidLoginCallback callback = conn.onInvalidLoginCallback();
      LoginInfo login = conn.login();
      ((ConnectionImpl) conn).localLogout();
      if (callback != null && callback.invalidLogin(conn, login)) {
        logger.fine("Solicitando que a chamada seja refeita.");
        throw new ForwardRequest(ri.target());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_other(ClientRequestInfo ri) {
  }
}
