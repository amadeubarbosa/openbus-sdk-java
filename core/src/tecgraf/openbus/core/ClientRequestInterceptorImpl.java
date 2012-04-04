package tecgraf.openbus.core;

import java.util.Arrays;
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
  /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
  private Map<String, ClientSideSession> sessions;
  /** Cache de cadeias assinadas */
  private Map<ChainCacheKey, SignedCallChain> chains;

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
    this.chains =
      Collections.synchronizedMap(new LRUCache<ChainCacheKey, SignedCallChain>(
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
      logger.finest(String.format("Realizando chamada fora do barramento: %s",
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
        logger.finest(String.format("utilizando sessão: id = %d ticket = %d",
          session.getSession(), ticket));
        byte[] secret;
        try {
          secret = session.getDecryptedSecret(conn);
        }
        catch (CryptographyException e) {
          String message = "Falha inesperada ao descriptografar segredo.";
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message);
        }
        byte[] credentialDataHash =
          this.generateCredentialDataHash(ri, secret, ticket);
        SignedCallChain chain = getCallChain(ri, conn, callee);
        logger.fine(String.format("Realizando chamada via barramento: %s",
          operation));
        return new CredentialData(busId, loginId, session.getSession(), ticket,
          credentialDataHash, chain);
      }
    }
    logger.finest(String.format("Realizando chamada sem credencial: %s",
      operation));
    return new CredentialData(busId, loginId, 0, 0,
      Cryptography.NULL_HASH_VALUE, Cryptography.NULL_SIGNED_CALL_CHAIN);
  }

  /**
   * Recupera a cadeia assinada que deve ser anexada a requisição.
   * 
   * @param ri informação do request
   * @param conn a conexão em uso
   * @param callee o alvo do request
   * @return A cadeia assinada.
   */
  private SignedCallChain getCallChain(ClientRequestInfo ri,
    ConnectionImpl conn, String callee) {
    SignedCallChain callChain;
    if (callee.equals(conn.busid())) {
      callChain = getSignedChain(ri);
    }
    else {
      try {
        // checando se existe na cache
        ChainCacheKey key =
          new ChainCacheKey(conn.login().id, callee, getSignedChain(ri));
        callChain = chains.get(key);
        if (callChain == null) {
          callChain = conn.access().signChainFor(callee);
          chains.put(key, callChain);
        }
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
   * Recupera a cadeia assinada que deve ser anexada a requisição
   * 
   * @param ri informação do request
   * @return A cadeia que esta joined ou uma cadeia nula.
   */
  private SignedCallChain getSignedChain(ClientRequestInfo ri) {
    SignedCallChain callChain;
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
      logger.finest(String.format("ForwardRequest após reset: %s", ri
        .operation()));
      throw new ForwardRequest(ri.target());
    }
    else if (exception.minor == InvalidLoginCode.value) {
      ConnectionImpl conn = (ConnectionImpl) this.getCurrentConnection(ri);
      InvalidLoginCallback callback = conn.onInvalidLoginCallback();
      LoginInfo login = conn.login();
      conn.localLogout();
      if (callback != null && callback.invalidLogin(conn, login)) {
        logger.finest(String.format("ForwardRequest após callback: %s", ri
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

  /**
   * Chave da cache de cadeias assinadas.
   * 
   * @author Tecgraf
   */
  private class ChainCacheKey {
    /**
     * O prório login
     */
    private String login;
    /**
     * O login do alvo da requisição
     */
    private String callee;
    /**
     * A cadeia com a qual esta joined
     */
    private SignedCallChain joinedChain;

    /**
     * Construtor.
     * 
     * @param login login.
     * @param callee login do alvo da requisição
     * @param joinedChain cadeia que esta joined
     */
    public ChainCacheKey(String login, String callee,
      SignedCallChain joinedChain) {
      this.login = login;
      this.callee = callee;
      this.joinedChain = joinedChain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ChainCacheKey) {
        ChainCacheKey other = (ChainCacheKey) obj;
        if (this.callee.equals(other.callee)
          && this.login.equals(other.login)
          && Arrays.equals(this.joinedChain.encoded, other.joinedChain.encoded)
          && Arrays.equals(this.joinedChain.signature,
            other.joinedChain.signature)) {
          return true;
        }
      }
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      // um valor qualquer
      int BASE = 17;
      // um valor qualquer
      int SEED = 37;
      int hash = BASE;
      if (this.login != null) {
        hash = hash * SEED + this.login.hashCode();
      }
      if (this.callee != null) {
        hash = hash * SEED + this.login.hashCode();
      }
      hash += Arrays.hashCode(this.joinedChain.encoded);
      hash += Arrays.hashCode(this.joinedChain.signature);
      return hash;
    }

  }
}
