package tecgraf.openbus.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.NO_PERMISSIONHelper;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UserException;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.Connection;
import tecgraf.openbus.core.Credential.Chain;
import tecgraf.openbus.core.Credential.Reset;
import tecgraf.openbus.core.Session.ClientSideSession;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.BusLogin;
import tecgraf.openbus.core.v2_1.credential.CredentialContextId;
import tecgraf.openbus.core.v2_1.credential.CredentialData;
import tecgraf.openbus.core.v2_1.credential.CredentialReset;
import tecgraf.openbus.core.v2_1.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidChainCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidTargetCode;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfoHolder;
import tecgraf.openbus.core.v2_1.services.access_control.NoCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnavailableBusCode;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.interceptors.CallChainInfo;
import tecgraf.openbus.interceptors.CallChainInfoHelper;
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
  private final Map<Integer, ConnectionImpl> uniqueId2Conn;
  /** Mapa interno do interceptador que associa o loginId ao requestId */
  private final Map<Integer, String> uniqueId2LoginId;

  /**
   * Construtor.
   * 
   * @param name nome do interceptador
   * @param mediator o mediador do ORB
   */
  ClientRequestInterceptorImpl(String name, ORBMediator mediator) {
    super(name, mediator);
    this.uniqueId2Conn =
      Collections.synchronizedMap(new HashMap<>());
    this.uniqueId2LoginId =
      Collections.synchronizedMap(new HashMap<>());
  }

  /**
   * Verifica se o booleando do slot está configurado.
   * 
   * @param ri informação da requisição
   * @param slotId identificador do slot
   * @return <code>true</code> se o slot esta ativado, e <code>false</code> caso
   *         contrário.
   */
  private boolean checkSlotIdFlag(ClientRequestInfo ri, int slotId) {
    try {
      Any any = ri.get_slot(slotId);
      if (any.type().kind().value() != TCKind._tk_null) {
        return any.extract_boolean();
      }
      return false;
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar slot";
      throw new INTERNAL(message);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_request(ClientRequestInfo ri) {
    String operation = ri.operation();
    if (checkSlotIdFlag(ri, context().getIgnoreThreadSlotId())) {
      logger.finest(String.format(
        "Realizando chamada fora do barramento: operação (%s)", operation));
      return;
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

    try {
      Credential credential = this.generateCredential(ri, conn, holder);
      if (credential.legacy != null) {
        ri.add_request_service_context(credential.toServiceContext(orb(),
          codec()), false);
      }
      else {
        // enviando credencial para início do handshake 
        Credential curr =
          new Credential(new CredentialData(credential.bus, credential.login,
            credential.session, credential.ticket, credential.hash,
            NULL_SIGNED_CALL_CHAIN));
        ri.add_request_service_context(curr.toServiceContext(orb(), codec()),
          false);
        if (conn.legacy()) {
          Credential legacy =
            new Credential(
              new tecgraf.openbus.core.v2_0.credential.CredentialData(
                credential.bus, credential.login, credential.session,
                credential.ticket, credential.hash,
                NULL_SIGNED_LEGACY_CALL_CHAIN));
          ri.add_request_service_context(legacy
            .toServiceContext(orb(), codec()), false);
        }
      }
    }
    catch (InvalidTypeForEncoding e) {
      String message = "Falha ao codificar a credencial ";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    // salvando informações da conexão e login que foram utilizados no request
    int uniqueId = mediator().getUniqueId();
    Any uniqueAny = orb().create_any();
    uniqueAny.insert_long(uniqueId);
    try {
      Current current = ORBUtils.getPICurrent(orb());
      current.set_slot(this.mediator().getUniqueIdSlot(), uniqueAny);
      uniqueId2Conn.put(uniqueId, conn);
      uniqueId2LoginId.put(uniqueId, currLogin.id);
      logger.finest(String.format(
        "associando o ID '%d' com o login '%s'. operação (%s)", uniqueId,
        holder.value.id, operation));
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot de uniqueId";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
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
  private Credential generateCredential(ClientRequestInfo ri,
    ConnectionImpl conn, LoginInfoHolder holder) {
    String operation = ri.operation();
    String bus = conn.busid();
    EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
    String targetId = conn.cache.entities.get(ep);
    if (targetId != null) {
      ClientSideSession session = conn.cache.cltSessions.get(targetId);
      if (session != null) {
        Chain chain = getCallChain(ri, conn, holder, targetId, session);
        boolean isLegacy = chain.isLegacy();
        int ticket = session.nextTicket();
        byte[] credentialDataHash =
          this.generateCredentialDataHash(ri, session.getSecret(), ticket,
            isLegacy);
        logger.finest(String.format("utilizando sessão: id = %d ticket = %d",
          session.getSession(), ticket));
        logger.fine(String.format(
          "Realizando chamada via barramento: target (%s) operação (%s)",
          targetId, operation));
        return new Credential(bus, holder.value.id, session.getSession(),
          ticket, credentialDataHash, chain, isLegacy);
      }
    }
    logger.finest(String.format(
      "Realizando chamada sem credencial: login (%s) operação (%s)",
      holder.value.id, operation));
    return new Credential(bus, holder.value.id, 0, 0, NULL_HASH_VALUE, null,
      null);
  }

  /**
   * Recupera a cadeia assinada que deve ser anexada a requisição.
   * 
   * @param ri informação do request
   * @param conn a conexão em uso
   * @param holder o login em uso.
   * @param target identificador do alvo do request
   * @param session a sessão em uso pela requisição
   * @return A cadeia assinada.
   */
  private Chain getCallChain(ClientRequestInfo ri, ConnectionImpl conn,
    LoginInfoHolder holder, String target, ClientSideSession session) {
    Chain joined = getJoinedChain(ri);
    if (target.equals(BusLogin.value)) {
      return joined;
    }
    else {
      try {
        String entity = session.getEntity();
        boolean legacy = session.legacy();
        LoginInfo curr = holder.value;
        ChainCacheKey key =
          new ChainCacheKey(entity, joined.signature(), legacy);
        Chain chain = conn.cache.chains.get(key);
        if (chain == null) {
          if (legacy) {
            SignedCallChain chainFor;
            do {
              chainFor = conn.legacySupport().access().signChainFor(target);
              curr = conn.getLogin();
            } while (!decodeSignedLegacyChain(chainFor).caller.id
              .equals(curr.id));
            chain = new Chain(chainFor);
            chain.updateInfos(conn.busid(), decodeSignedLegacyChain(chainFor));
            conn.cache.chains.put(key, chain);
          }
          else if (joined.isLegacy() && !conn.legacy()) {
            String error = "não é possível unir-se a cadeia legada";
            logger.log(Level.SEVERE, error);
            throw new NO_PERMISSION(error, InvalidChainCode.value,
              CompletionStatus.COMPLETED_NO);
          }
          else {
            SignedData chainFor;
            do {
              chainFor = conn.access().signChainFor(entity);
              curr = conn.getLogin();
            } while (!decodeSignedChain(chainFor).caller.id.equals(curr.id));
            chain = new Chain(chainFor);
            chain.updateInfos(decodeSignedChain(chainFor));
            conn.cache.chains.put(key, chain);
          }
        }
        holder.value = curr;
        return chain;
      }
      catch (InvalidLogins e) {
        Map<EffectiveProfile, String> cache = conn.cache.entities;
        List<EffectiveProfile> toRemove = new ArrayList<>();
        for (Entry<EffectiveProfile, String> entry : cache.entrySet()) {
          if (target.equals(entry.getValue())) {
            toRemove.add(entry.getKey());
          }
        }
        for (EffectiveProfile ep : toRemove) {
          cache.remove(ep);
        }
        String message =
          String.format("Erro ao assinar cadeia para target: (%s)",
            e.loginIds[0]);
        logger.log(Level.SEVERE, message, e);
        throw new NO_PERMISSION(message, InvalidTargetCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      catch (SystemException e) {
        String message =
          String
            .format("Erro durante acesso ao barramento (%s).", conn.busid());
        logger.log(Level.SEVERE, message, e);
        throw new NO_PERMISSION(message, UnavailableBusCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      catch (ServiceFailure e) {
        String message =
          String.format(
            "Falha inesperada ao assinar uma cadeia de chamadas: target (%s)",
            target);
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
      catch (UserException e) {
        String message = "Falha inesperada ao decodificar cadeia";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
    }
  }

  /**
   * Decodifica a cadeia
   * 
   * @param data o dado codificado
   * @return a cadeia
   * @throws FormatMismatch
   * @throws TypeMismatch
   */
  private CallChain decodeSignedChain(SignedData data) throws FormatMismatch,
    TypeMismatch {
    Any any = codec().decode_value(data.encoded, CallChainHelper.type());
    return CallChainHelper.extract(any);
  }

  /**
   * Decodifica a cadeia
   * 
   * @param data o dado codificado
   * @return a cadeia
   * @throws FormatMismatch
   * @throws TypeMismatch
   */
  private tecgraf.openbus.core.v2_0.services.access_control.CallChain decodeSignedLegacyChain(
    SignedCallChain data) throws FormatMismatch, TypeMismatch {
    Any any =
      codec().decode_value(
        data.encoded,
        tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper
          .type());
    return tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper
      .extract(any);
  }

  /**
   * Recupera a cadeia assinada que deve ser anexada a requisição
   * 
   * @param ri informação do request
   * @return A cadeia que esta joined ou uma cadeia nula.
   */
  private Chain getJoinedChain(ClientRequestInfo ri) {
    Chain chain;
    try {
      Any any = ri.get_slot(this.mediator().getJoinedChainSlotId());
      if (any.type().kind().value() != TCKind._tk_null) {
        CallChainInfo info = CallChainInfoHelper.extract(any);
        CallerChainImpl callerChain =
          CallerChainImpl.info2CallerChain(info, codec());
        chain = callerChain.internal_chain();
      }
      else {
        chain = new Chain(NULL_SIGNED_CALL_CHAIN);
      }
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot do JoinedChain";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao decodificar cadeia";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    return chain;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_poll(ClientRequestInfo ri) {
    logger.finest(String.format("[inout] send_pool: %s", ri.operation()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_reply(ClientRequestInfo ri) {
    String operation = ri.operation();
    logger.finest(String.format("[in] receive_reply: %s", operation));
    if (!checkSlotIdFlag(ri, context().getIgnoreThreadSlotId())) {
      clearRequestUniqueId();
      logger.fine(String.format(
        "requisição atendida com sucesso: operação (%s)", ri.operation()));
    }
    logger.finest(String.format("[out] receive_reply: %s", operation));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    Integer uniqueId;
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

      uniqueId = getRequestUniqueId();
      ConnectionImpl conn = uniqueId2Conn.get(uniqueId);
      String loginId = uniqueId2LoginId.get(uniqueId);
      switch (exception.minor) {

        case InvalidCredentialCode.value:
          if (conn == null || loginId == null) {
            String message = "Faltam informações associadas a requisição";
            logger.log(Level.SEVERE, message);
            throw new INTERNAL(message);
          }
          EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
          Reset reset = getCredentialReset(ri, conn.legacy());
          if (reset == null) {
            // não recebeu o credential reset
            String message =
              "Servidor não enviou o CredentialReset para negociar sessão)";
            logger.info(message);
            throw new NO_PERMISSION(message, InvalidRemoteCode.value,
              CompletionStatus.COMPLETED_NO);
          }

          Cryptography crypto = Cryptography.getInstance();
          byte[] secret;
          try {
            secret = crypto.decrypt(reset.challenge, conn.getPrivateKey());
          }
          catch (CryptographyException e) {
            String message = "Falha inesperada ao descriptografar segredo.";
            logger.log(Level.SEVERE, message, e);
            throw new NO_PERMISSION(message, InvalidRemoteCode.value,
              CompletionStatus.COMPLETED_NO);
          }
          conn.cache.entities.put(ep, reset.target);
          conn.cache.cltSessions.put(reset.target, new ClientSideSession(reset,
            secret));
          logger.finest(String
            .format("ForwardRequest: login (%s) operação (%s)", loginId, ri
              .operation()));
          throw new ForwardRequest(ri.target());

        case InvalidLoginCode.value:
          boolean skipInvLogin =
            checkSlotIdFlag(ri, context().getSkipInvalidLoginSlotId());
          if (!skipInvLogin) {
            if (conn == null || loginId == null) {
              String message = "Faltam informações associadas a requisição";
              logger.log(Level.SEVERE, message);
              throw new INTERNAL(message);
            }
            int validity = 0;
            try {
              context().ignoreInvLogin();
              validity = context().getLoginRegistry().getLoginValidity(loginId);
            }
            catch (NO_PERMISSION e) {
              if (e.minor == InvalidLoginCode.value) {
                LoginInfo curr = conn.login();
                if (curr != null && curr.id.equals(loginId)) {
                  conn.localLogout(true);
                }
                logger.finest(String.format(
                  "ForwardRequest: login (%s) operação (%s)", loginId, ri
                    .operation()));
                throw new ForwardRequest(ri.target());
              }
              else {
                String msg =
                  String.format(
                    "Erro em validação de login: login (%s) operação (%s)",
                    loginId, ri.operation());
                logger.log(Level.SEVERE, msg, e);
                throw new NO_PERMISSION(msg, UnavailableBusCode.value,
                  CompletionStatus.COMPLETED_NO);
              }
            }
            catch (Exception e) {
              String msg =
                String.format(
                  "Erro em validação de login: login (%s) operação (%s)",
                  loginId, ri.operation());
              logger.log(Level.SEVERE, msg, e);
              throw new NO_PERMISSION(msg, UnavailableBusCode.value,
                CompletionStatus.COMPLETED_NO);
            }
            finally {
              context().unignoreInvLogin();
            }
            if (validity > 0) {
              String msg =
                String.format(
                  "InvalidLogin equivocado: login (%s) operação (%s)", loginId,
                  ri.operation());
              logger.info(msg);
              throw new NO_PERMISSION(msg, InvalidRemoteCode.value,
                CompletionStatus.COMPLETED_NO);
            }
            else {
              logger.finest(String.format(
                "ForwardRequest: login (%s) operação (%s)", loginId, ri
                  .operation()));
              throw new ForwardRequest(ri.target());
            }
          }
          break;

        case NoLoginCode.value:
        case UnavailableBusCode.value:
        case InvalidTargetCode.value:
        case InvalidRemoteCode.value:
          String message =
            "Servidor repassou uma exceção NO_PERMISSION local: minor = %d";
          String msg = String.format(message, exception.minor);
          logger.log(Level.WARNING, msg, exception);
          throw new NO_PERMISSION(msg, InvalidRemoteCode.value,
            CompletionStatus.COMPLETED_NO);

        case NoCredentialCode.value:
          // deixa a exceção passar
          break;

        default:
          // deixa a exceção passar
          break;
      }
    }
    finally {
      clearRequestUniqueId();
    }
  }

  /**
   * Recupera a informação do CredentialReset
   * 
   * @param ri informações da requisição
   * @param legacy se a conexão possui o suporte legado ativo
   * @return as informações de Reset do handshake
   */
  private Reset getCredentialReset(ClientRequestInfo ri, boolean legacy) {
    try {
      byte[] context_data = null;
      try {
        ServiceContext context =
          ri.get_reply_service_context(CredentialContextId.value);
        context_data = context.context_data;
      }
      catch (BAD_PARAM e) {
        // (if 26) request's service context does not contain an entry
        // for that ID.
        if (e.minor != 26) {
          throw e;
        }
      }
      if (context_data != null) {
        Any any =
          codec().decode_value(context_data, CredentialResetHelper.type());
        CredentialReset creset = CredentialResetHelper.extract(any);
        return new Reset(new LoginInfo(creset.target, creset.entity),
          creset.session, creset.challenge, false);
      }
      else if (legacy) {
        try {
          ServiceContext context =
            ri.get_reply_service_context(tecgraf.openbus.core.v2_0.credential.CredentialContextId.value);
          context_data = context.context_data;
        }
        catch (BAD_PARAM e) {
          // (if 26) request's service context does not contain an entry
          // for that ID.
          if (e.minor != 26) {
            throw e;
          }
        }
        if (context_data != null) {
          Any any =
            codec()
              .decode_value(
                context_data,
                tecgraf.openbus.core.v2_0.credential.CredentialResetHelper
                  .type());
          tecgraf.openbus.core.v2_0.credential.CredentialReset creset =
            tecgraf.openbus.core.v2_0.credential.CredentialResetHelper
              .extract(any);
          return new Reset(new LoginInfo(creset.target, null), creset.session,
            creset.challenge, true);
        }
      }
    }
    catch (FormatMismatch | TypeMismatch e) {
      String message = "Falha inesperada ao obter o CredentialReset";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    return null;
  }

  /**
   * Recupera o identificador único associado ao request.
   * 
   * @return o identificador associado.
   */
  private Integer getRequestUniqueId() {
    try {
      Current current = ORBUtils.getPICurrent(orb());
      Any uniqueAny = current.get_slot(this.mediator().getUniqueIdSlot());
      if (uniqueAny.type().kind().value() != TCKind._tk_null) {
        return uniqueAny.extract_long();
      }
      else {
        String message = "Any de chave única de requestId está vazia!";
        logger.fine(message);
        return null;
      }
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot do request Id";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * Libera os recursos associados ao identificador de request.
   */
  private void clearRequestUniqueId() {
    Integer uniqueId = getRequestUniqueId();
    if (uniqueId != null) {
      uniqueId2Conn.remove(uniqueId);
      uniqueId2LoginId.remove(uniqueId);
      try {
        ORB orb = orb();
        Any emptyAny = orb.create_any();
        Current current = ORBUtils.getPICurrent(orb);
        current.set_slot(this.mediator().getUniqueIdSlot(), emptyAny);
      }
      catch (InvalidSlot e) {
        String message = "Falha inesperada ao acessar o slot de requestId";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
    }
  }

  /**
   * Recupera a conexão corrente.
   * 
   * @param ri informação do request
   * @return a conexão.
   */
  private Connection getCurrentConnection(RequestInfo ri) {
    OpenBusContextImpl context = this.mediator().getContext();
    Any any;
    try {
      any = ri.get_slot(context.getCurrentConnectionSlotId());
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot da conexão corrente";
      throw new INTERNAL(message);
    }
    if (any.type().kind().value() != TCKind._tk_null) {
      int id = any.extract_long();
      Connection connection = context.getConnectionById(id);
      if (connection != null) {
        return connection;
      }
    }
    Connection connection = context.defaultConnection();
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
    logger.finest(String.format("[inout] receive_other: %s", ri.operation()));
  }

}
