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
import org.omg.CORBA.TCKind;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;

import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.Session.ClientSideSession;
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
import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChainHelper;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.LRUCache;

/**
 * Implementa��o do interceptador cliente.
 * 
 * @author Tecgraf
 */
final class ClientRequestInterceptorImpl extends InterceptorImpl implements
  ClientRequestInterceptor {

  /** Inst�ncia de logging. */
  private static final Logger logger = Logger
    .getLogger(ClientRequestInterceptorImpl.class.getName());

  /** Mapa de profile do interceptador para o cliente alvo da chamanha */
  private Map<EffectiveProfile, String> entities;
  /** Cache de sess�o: mapa de cliente alvo da chamada para sess�o */
  private Map<String, ClientSideSession> sessions;

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
    this.sessions =
      Collections.synchronizedMap(new LRUCache<String, ClientSideSession>(
        CACHE_SIZE));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_request(ClientRequestInfo ri) {
    String operation = ri.operation();
    BusORBImpl orb = (BusORBImpl) this.getMediator().getORB();
    if (orb.isCurrentThreadIgnored()) {
      logger.fine(String.format("Realizando chamada fora do barramento: %s",
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
   * @param ri Informa��o do request
   * @return A credencial v�lida para a sess�o, ou uma credencial para for�ar o
   *         reset da sess�o.
   */
  private CredentialData generateCredentialData(ClientRequestInfo ri) {
    String operation = ri.operation();
    ConnectionImpl conn = (ConnectionImpl) this.getCurrentConnection(ri);
    String busId = conn.busid();
    String loginId = conn.login().id;
    EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
    String callee = this.entities.get(ep);
    if (callee != null) {
      ClientSideSession session = this.sessions.get(callee);
      if (session != null) {
        int ticket = session.nextTicket();
        logger.finest(String.format("utilizando sess�o: id = %d ticket = %d",
          session.getSession(), ticket));
        byte[] secret;
        try {
          secret = session.getDecryptedSecret(conn);
        }
        catch (CryptographyException e) {
          String message = "Falha inesperada descriptografar segredo.";
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message);
        }
        byte[] credentialDataHash =
          this.generateCredentialDataHash(ri, secret, ticket);
        SignedCallChain chain = getCallChain(ri, conn, callee);
        logger.info(String.format("Realizando chamada via barramento: %s",
          operation));
        return new CredentialData(busId, loginId, session.getSession(), ticket,
          credentialDataHash, chain);
      }
    }
    logger.fine(String.format("Realizando chamada sem credencial: %s",
      operation));
    return new CredentialData(busId, loginId, 0, 0,
      Cryptography.NULL_HASH_VALUE, Cryptography.NULL_SIGNED_CALL_CHAIN);
  }

  /**
   * Recupera a cadeia assinada que deve ser anexada a requisi��o.
   * 
   * @param ri informa��o do request
   * @param conn a conex�o em uso
   * @param callee o alvo do request
   * @return A cadeia assinada.
   */
  private SignedCallChain getCallChain(ClientRequestInfo ri,
    ConnectionImpl conn, String callee) {
    SignedCallChain callChain;
    if (callee.equals(conn.busid())) {
      try {
        Any any = ri.get_slot(this.getMediator().getJoinedChainSlotId());
        if (any.type().kind().value() != TCKind._tk_null) {
          callChain = SignedCallChainHelper.extract(any);
        }
        else {
          callChain = Cryptography.NULL_SIGNED_CALL_CHAIN;
        }
      }
      catch (InvalidSlot e) {
        String message = "Falha inesperada ao obter o slot do JoinedChain";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
    }
    else {
      try {
        callChain = conn.access().signChainFor(callee);
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
    return callChain;
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
    if (!ri.received_exception_id().equals(NO_PERMISSIONHelper.id())) {
      return;
    }

    Any exceptionAny = ri.received_exception();
    NO_PERMISSION exception = NO_PERMISSIONHelper.extract(exceptionAny);
    if (!exception.completed.equals(CompletionStatus.COMPLETED_NO)) {
      return;
    }

    if (exception.minor == InvalidCredentialCode.value) {
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
      this.sessions.put(callee, new ClientSideSession(reset.session,
        reset.challenge, reset.login));
      logger.fine(String
        .format("ForwardRequest ap�s reset: %s", ri.operation()));
      throw new ForwardRequest(ri.target());
    }
    else if (exception.minor == InvalidLoginCode.value) {
      ConnectionImpl conn = (ConnectionImpl) this.getCurrentConnection(ri);
      InvalidLoginCallback callback = conn.onInvalidLoginCallback();
      LoginInfo login = conn.login();
      conn.localLogout();
      if (callback != null && callback.invalidLogin(conn, login)) {
        logger.fine(String.format("ForwardRequest ap�s callback: %s", ri
          .operation()));
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
