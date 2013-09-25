package tecgraf.openbus.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.NO_PERMISSIONHelper;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.Connection;
import tecgraf.openbus.core.Session.ClientSideSession;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.CredentialHelper;
import tecgraf.openbus.core.v2_0.BusLogin;
import tecgraf.openbus.core.v2_0.credential.CredentialContextId;
import tecgraf.openbus.core.v2_0.credential.CredentialData;
import tecgraf.openbus.core.v2_0.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_0.credential.CredentialReset;
import tecgraf.openbus.core.v2_0.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.credential.SignedCallChainHelper;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidTargetCode;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfoHolder;
import tecgraf.openbus.core.v2_0.services.access_control.NoCredentialCode;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnavailableBusCode;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.security.Cryptography;

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

  /** Mapa interno do interceptador que associa a conexão ao requestId */
  private Map<Integer, ConnectionImpl> requestId2Conn;
  /** Mapa interno do interceptador que associa o loginId ao requestId */
  private Map<Integer, String> requestId2LoginId;

  /**
   * Construtor.
   * 
   * @param name nome do interceptador
   * @param mediator o mediador do ORB
   */
  ClientRequestInterceptorImpl(String name, ORBMediator mediator) {
    super(name, mediator);
    this.requestId2Conn =
      Collections.synchronizedMap(new HashMap<Integer, ConnectionImpl>());
    this.requestId2LoginId =
      Collections.synchronizedMap(new HashMap<Integer, String>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_request(ClientRequestInfo ri) {
    String operation = ri.operation();
    ORB orb = this.getMediator().getORB();
    OpenBusContextImpl manager;
    try {
      org.omg.CORBA.Object obj =
        orb.resolve_initial_references("OpenBusContext");
      manager = (OpenBusContextImpl) obj;
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o multiplexador";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    Codec codec = this.getMediator().getCodec();
    if (manager.isCurrentThreadIgnored(ri)) {
      logger.finest(String.format(
        "Realizando chamada fora do barramento: operação (%s)", operation));
      return;
    }

    boolean joinedToLegacy = false;
    SignedCallChain joinedChain = getSignedChain(ri);
    if (Arrays.equals(joinedChain.signature, LEGACY_ENCRYPTED_BLOCK)) {
      // joined com cadeia 1.5;
      joinedToLegacy = true;
    }

    ConnectionImpl conn = (ConnectionImpl) this.getCurrentConnection(ri);
    LoginInfo currLogin = conn.getLogin();
    if (currLogin == null) {
      String message =
        String.format(
          "Chamada cancelada. Conexão não possui login. operação (%s)",
          operation);
      logger.info(message);
      throw new NO_PERMISSION(message, NoLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    LoginInfoHolder holder = new LoginInfoHolder();
    holder.value = currLogin;

    // salvando a conexão e o login utilizado no request.

    // montando credencial 2.0
    if (!joinedToLegacy) {
      CredentialData credential = this.generateCredentialData(ri, conn, holder);
      Any anyCredential = orb.create_any();
      CredentialDataHelper.insert(anyCredential, credential);
      byte[] encodedCredential;
      try {
        encodedCredential = codec.encode_value(anyCredential);
      }
      catch (InvalidTypeForEncoding e) {
        String message = "Falha ao codificar a credencial";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
      ServiceContext requestServiceContext =
        new ServiceContext(CredentialContextId.value, encodedCredential);
      ri.add_request_service_context(requestServiceContext, false);
    }

    if (conn.legacy()) {
      try {
        // construindo credencial 1.5
        Credential legacyCredential = new Credential();
        legacyCredential.identifier = currLogin.id;
        legacyCredential.owner = currLogin.entity;
        String delegate = "";

        if (joinedChain != NULL_SIGNED_CALL_CHAIN) {
          Any anyChain =
            codec.decode_value(joinedChain.encoded, CallChainHelper.type());
          CallChain chain = CallChainHelper.extract(anyChain);
          if (chain.originators != null && chain.originators.length > 0
            && conn.isLegacyDelegateSetToOriginator()) {
            delegate = chain.originators[0].entity;
          }
          else {
            delegate = chain.caller.entity;
          }
        }
        legacyCredential.delegate = delegate;
        Any anyLegacy = orb.create_any();
        CredentialHelper.insert(anyLegacy, legacyCredential);
        byte[] encodedLegacy = codec.encode_value(anyLegacy);
        int legacyContextId = 1234;
        ServiceContext legacyServiceContext =
          new ServiceContext(legacyContextId, encodedLegacy);
        ri.add_request_service_context(legacyServiceContext, false);
      }
      catch (TypeMismatch e) {
        String message = "Falha ao construir a credencial 1.5";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
      catch (FormatMismatch e) {
        String message = "Falha ao construir a credencial 1.5";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
      catch (InvalidTypeForEncoding e) {
        String message = "Falha ao codificar a credencial 1.5";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
    }

    if (joinedToLegacy && !conn.legacy()) {
      String message =
        "Impossível construir credencial: joined em cadeia 1.5 e sem suporte a legacy";
      logger.severe(message);
      throw new INTERNAL(message);
    }
    // salvando informações da conexão e login que foram utilizados no request
    requestId2Conn.put(Integer.valueOf(ri.request_id()), conn);
    requestId2LoginId.put(Integer.valueOf(ri.request_id()), currLogin.id);
  }

  /**
   * Gera uma credencial para a chamada.
   * 
   * @param ri Informação do request
   * @param conn A conexão em uso.
   * @param holder o login em uso.
   * @return A credencial válida para a sessão, ou uma credencial para forçar o
   *         reset da sessão.
   */
  private CredentialData generateCredentialData(ClientRequestInfo ri,
    ConnectionImpl conn, LoginInfoHolder holder) {
    String operation = ri.operation();
    String busId = conn.busid();
    EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
    String target = conn.cache.entities.get(ep);
    if (target != null) {
      ClientSideSession session = conn.cache.cltSessions.get(target);
      if (session != null) {
        int ticket = session.nextTicket();
        logger.finest(String.format("utilizando sessão: id = %d ticket = %d",
          session.getSession(), ticket));
        byte[] secret = session.getSecret();
        byte[] credentialDataHash =
          this.generateCredentialDataHash(ri, secret, ticket);
        SignedCallChain chain = getCallChain(ri, conn, holder, target);
        logger.fine(String.format(
          "Realizando chamada via barramento: target (%s) operação (%s)",
          target, operation));
        return new CredentialData(busId, holder.value.id, session.getSession(),
          ticket, credentialDataHash, chain);
      }
    }
    logger.finest(String.format(
      "Realizando chamada sem credencial: login (%s) operação (%s)",
      holder.value.id, operation));
    return new CredentialData(busId, holder.value.id, 0, 0, NULL_HASH_VALUE,
      NULL_SIGNED_CALL_CHAIN);
  }

  /**
   * Recupera a cadeia assinada que deve ser anexada a requisição.
   * 
   * @param ri informação do request
   * @param conn a conexão em uso
   * @param holder o login em uso.
   * @param target o alvo do request
   * @return A cadeia assinada.
   */
  private SignedCallChain getCallChain(ClientRequestInfo ri,
    ConnectionImpl conn, LoginInfoHolder holder, String target) {
    SignedCallChain callChain;
    if (target.equals(BusLogin.value)) {
      callChain = getSignedChain(ri);
    }
    else {
      try {
        LoginInfo curr = holder.value;
        // checando se existe na cache
        ChainCacheKey key =
          new ChainCacheKey(curr.id, target, getSignedChain(ri));
        callChain = conn.cache.chains.get(key);
        if (callChain == null) {
          do {
            callChain = conn.access().signChainFor(target);
            curr = conn.getLogin();
          } while (!unmarshallSignedChain(callChain, logger).caller.id
            .equals(curr.id));
          conn.cache.chains.put(key, callChain);
        }
        holder.value = curr;
      }
      catch (SystemException e) {
        String message =
          String
            .format("Erro durante acesso ao barramento (%s).", conn.busid());
        logger.log(Level.SEVERE, message, e);
        throw new NO_PERMISSION(message, UnavailableBusCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      catch (InvalidLogins e) {
        String message =
          String.format("Erro ao assinar cadeia para target: (%s)",
            e.loginIds[0]);
        logger.log(Level.SEVERE, message, e);
        throw new NO_PERMISSION(message, InvalidTargetCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      catch (ServiceFailure e) {
        String message =
          String
            .format(
              "Falha inesperada ao assinar uma cadeia de chamadas para o target (%s)",
              target);
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
        callChain = NULL_SIGNED_CALL_CHAIN;
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
    Integer requestId = Integer.valueOf(ri.request_id());
    this.requestId2Conn.remove(requestId);
    this.requestId2LoginId.remove(requestId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    Integer requestId = Integer.valueOf(ri.request_id());
    try {
      logger.finest(String.format("Exception: %s Request: %s", ri
        .received_exception_id(), ri.operation()));
      if (!ri.received_exception_id().equals(NO_PERMISSIONHelper.id())) {
        return;
      }

      Any exceptionAny = ri.received_exception();
      NO_PERMISSION exception = NO_PERMISSIONHelper.extract(exceptionAny);
      if (!exception.completed.equals(CompletionStatus.COMPLETED_NO)) {
        return;
      }

      ConnectionImpl conn = requestId2Conn.get(requestId);
      switch (exception.minor) {

        case InvalidCredentialCode.value:
          EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
          ServiceContext context =
            ri.get_reply_service_context(CredentialContextId.value);
          if (context.context_data == null) {
            // não recebeu o credential reset
            String message =
              "Servidor chamado é inválido (servidor não enviou o CredentialReset para negociar sessão)";
            logger.info(message);
            throw new NO_PERMISSION(message, InvalidRemoteCode.value,
              CompletionStatus.COMPLETED_NO);
          }
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

          CredentialReset reset =
            CredentialResetHelper.extract(credentialResetAny);
          String target = reset.target;
          Cryptography crypto = Cryptography.getInstance();
          byte[] secret;
          try {
            secret = crypto.decrypt(reset.challenge, conn.getPrivateKey());
          }
          catch (CryptographyException e) {
            String message = "Falha inesperada ao descriptografar segredo.";
            logger.log(Level.SEVERE, message, e);
            throw new INTERNAL(message);
          }
          conn.cache.entities.put(ep, target);
          conn.cache.cltSessions.put(target, new ClientSideSession(
            reset.session, secret, target));
          logger.finest(String.format(
            "Associando profile_data '%s'a entidade '%s'", ep, target));
          logger.finest(String.format("Associando entidade '%s'a sessão '%s'",
            target, reset.session));
          logger.finest(String.format("ForwardRequest após reset: %s", ri
            .operation()));
          throw new ForwardRequest(ri.target());

        case InvalidLoginCode.value:
          String loginId = requestId2LoginId.get(requestId);
          LoginInfo info = conn.login();
          if (info != null && loginId.equals(info.id)) {
            conn.localLogout(true);
          }
          logger.info(String.format("Recebeu exceção InvalidLogin: %s", ri
            .operation()));
          // tenta refazer o login.
          LoginInfo afterLogin = conn.getLogin();
          if (afterLogin == null) {
            throw new NO_PERMISSION("Callback não refez o login da conexão.",
              NoLoginCode.value, CompletionStatus.COMPLETED_NO);
          }

          // callback conseguiu refazer o login
          logger.finest(String.format("ForwardRequest após callback: %s", ri
            .operation()));
          throw new ForwardRequest(ri.target());

        case NoCredentialCode.value:
          String message =
            "Servidor chamado é inválido (não detectou credencial enviada)";
          logger.info(message);
          throw new NO_PERMISSION(message, InvalidRemoteCode.value,
            CompletionStatus.COMPLETED_NO);

        default:
          // deixa a exceção passar
          break;
      }
    }
    finally {
      this.requestId2Conn.remove(requestId);
      this.requestId2LoginId.remove(requestId);
    }
  }

  /**
   * Recupera a conexão corrente.
   * 
   * @param ri informação do request
   * @return a conexão.
   */
  protected Connection getCurrentConnection(RequestInfo ri) {
    OpenBusContextImpl context = this.getMediator().getContext();
    Any any;
    try {
      any = ri.get_slot(context.getCurrentConnectionSlotId());
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot da conexão corrente";
      throw new INTERNAL(message);
    }
    if (any.type().kind().value() != TCKind._tk_null) {
      long id = any.extract_longlong();
      Connection connection = context.getConnectionByThreadId(id);
      if (connection != null) {
        return connection;
      }
    }
    Connection connection = context.getDefaultConnection();
    if (connection != null) {
      return connection;
    }
    throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus.COMPLETED_NO);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_other(ClientRequestInfo ri) {
  }

}
