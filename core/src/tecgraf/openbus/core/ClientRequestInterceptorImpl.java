package tecgraf.openbus.core;

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
import tecgraf.openbus.core.Session.ClientSideSession;
import tecgraf.openbus.core.v2_1.BusLogin;
import tecgraf.openbus.core.v2_1.credential.CredentialContextId;
import tecgraf.openbus.core.v2_1.credential.CredentialData;
import tecgraf.openbus.core.v2_1.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_1.credential.CredentialReset;
import tecgraf.openbus.core.v2_1.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.credential.SignedDataHelper;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidTargetCode;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfoHolder;
import tecgraf.openbus.core.v2_1.services.access_control.NoCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnavailableBusCode;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.security.Cryptography;

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

  /** Mapa interno do interceptador que associa a conex�o ao requestId */
  private Map<Integer, ConnectionImpl> uniqueId2Conn;
  /** Mapa interno do interceptador que associa o loginId ao requestId */
  private Map<Integer, String> uniqueId2LoginId;

  /**
   * Construtor.
   * 
   * @param name nome do interceptador
   * @param mediator o mediador do ORB
   */
  ClientRequestInterceptorImpl(String name, ORBMediator mediator) {
    super(name, mediator);
    this.uniqueId2Conn =
      Collections.synchronizedMap(new HashMap<Integer, ConnectionImpl>());
    this.uniqueId2LoginId =
      Collections.synchronizedMap(new HashMap<Integer, String>());
  }

  /**
   * Informa se a requisi��o est� configurada para ignorar a intercepta��o.
   * 
   * @param ri informa��o da requisi��o
   * @return <code>true</code> se a requisi��o deve ser ignorada e
   *         <code>false</code> caso contr�rio.
   */
  private boolean isIgnoringThread(ClientRequestInfo ri) {
    OpenBusContextImpl manager;
    try {
      org.omg.CORBA.Object obj =
        orb().resolve_initial_references("OpenBusContext");
      manager = (OpenBusContextImpl) obj;
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o multiplexador";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    if (manager.isCurrentThreadIgnored(ri)) {
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_request(ClientRequestInfo ri) {
    String operation = ri.operation();
    if (isIgnoringThread(ri)) {
      logger.fine(String.format(
        "Realizando chamada fora do barramento: opera��o (%s)", operation));
      return;
    }
    try {
      logger.finest(String.format("[in] send_request: %s", operation));
      ConnectionImpl conn = (ConnectionImpl) this.getCurrentConnection(ri);
      LoginInfo currLogin = conn.getLogin();
      if (currLogin == null) {
        String message =
          String.format(
            "Chamada cancelada. Conex�o n�o possui login. opera��o (%s)",
            operation);
        logger.info(message);
        throw new NO_PERMISSION(message, NoLoginCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      LoginInfoHolder holder = new LoginInfoHolder();
      holder.value = currLogin;

      CredentialData credential = this.generateCredential(ri, conn, holder);
      byte[] encodedCredential = this.encodeCredential(credential);
      ServiceContext requestServiceContext =
        new ServiceContext(CredentialContextId.value, encodedCredential);
      ri.add_request_service_context(requestServiceContext, false);

      // salvando informa��es da conex�o e login que foram utilizados no request
      ORBMediator mediator = mediator();
      ORB orb = orb();
      int uniqueId = mediator.getUniqueId();
      Any uniqueAny = orb.create_any();
      uniqueAny.insert_long(uniqueId);
      try {
        Current current = ORBUtils.getPICurrent(orb);
        current.set_slot(mediator.getUniqueIdSlot(), uniqueAny);
        uniqueId2Conn.put(uniqueId, conn);
        uniqueId2LoginId.put(uniqueId, holder.value.id);
        logger.finest(String.format(
          "associando o ID '%d' com o login '%s'. opera��o (%s)", uniqueId,
          holder.value.id, operation));
      }
      catch (InvalidSlot e) {
        String message = "Falha inesperada ao obter o slot de uniqueId";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
    }
    finally {
      logger.finest(String.format("[out] send_request: %s", operation));
    }
  }

  /**
   * Codifica uma credencial
   * 
   * @param credential a credencial a ser codificada.
   * @return a credencial codificada.
   */
  private byte[] encodeCredential(CredentialData credential) {
    Any anyCredential = orb().create_any();
    CredentialDataHelper.insert(anyCredential, credential);
    try {
      return codec().encode_value(anyCredential);
    }
    catch (InvalidTypeForEncoding e) {
      String message = "Falha ao codificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * Gera uma credencial para a chamada.
   * 
   * @param ri Informa��o do request
   * @param conn A conex�o em uso.
   * @param holder o login em uso.
   * @return A credencial v�lida para a sess�o, ou uma credencial para for�ar o
   *         reset da sess�o.
   */
  private CredentialData generateCredential(ClientRequestInfo ri,
    ConnectionImpl conn, LoginInfoHolder holder) {
    String operation = ri.operation();
    String busId = conn.busid();
    EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
    String target = conn.cache.entities.get(ep);
    if (target != null) {
      ClientSideSession session = conn.cache.cltSessions.get(target);
      if (session != null) {
        int ticket = session.nextTicket();
        byte[] secret = session.getSecret();
        byte[] credentialDataHash =
          this.generateCredentialDataHash(ri, secret, ticket);
        SignedData chain = getCallChain(ri, conn, holder, target);
        logger.finest(String.format("utilizando sess�o: id = %d ticket = %d",
          session.getSession(), ticket));
        logger.fine(String.format(
          "Realizando chamada via barramento: target (%s) opera��o (%s)",
          target, operation));
        return new CredentialData(busId, holder.value.id, session.getSession(),
          ticket, credentialDataHash, chain);
      }
    }
    logger.finest(String.format(
      "Realizando chamada sem credencial: login (%s) opera��o (%s)",
      holder.value.id, operation));
    return new CredentialData(busId, holder.value.id, 0, 0, NULL_HASH_VALUE,
      NULL_SIGNED_CALL_CHAIN);
  }

  /**
   * Recupera a cadeia assinada que deve ser anexada a requisi��o.
   * 
   * @param ri informa��o do request
   * @param conn a conex�o em uso
   * @param holder o login em uso.
   * @param target o alvo do request
   * @return A cadeia assinada.
   */
  private SignedData getCallChain(ClientRequestInfo ri, ConnectionImpl conn,
    LoginInfoHolder holder, String target) {
    if (target.equals(BusLogin.value)) {
      return getJoinedChain(ri);
    }
    try {
      LoginInfo curr = holder.value;
      // checando se existe na cache
      ChainCacheKey key =
        new ChainCacheKey(curr.id, target, getJoinedChain(ri));
      SignedData callChain = conn.cache.chains.get(key);
      if (callChain == null) {
        do {
          callChain = conn.access().signChainFor(target);
          curr = conn.getLogin();
        } while (!decodeSignedChain(callChain, logger).caller.id
          .equals(curr.id));
        conn.cache.chains.put(key, callChain);
      }
      holder.value = curr;
      return callChain;
    }
    catch (SystemException e) {
      String message =
        String.format("Erro durante acesso ao barramento (%s).", conn.busid());
      logger.log(Level.SEVERE, message, e);
      throw new NO_PERMISSION(message, UnavailableBusCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (InvalidLogins e) {
      String message =
        String
          .format("Erro ao assinar cadeia para target: (%s)", e.loginIds[0]);
      logger.log(Level.SEVERE, message, e);
      throw new NO_PERMISSION(message, InvalidTargetCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (ServiceFailure e) {
      String message =
        String.format("Erro ao assinar cadeia para target: (%s)", target);
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * Recupera a cadeia assinada que deve ser anexada a requisi��o
   * 
   * @param ri informa��o do request
   * @return A cadeia que esta joined ou uma cadeia nula.
   */
  private SignedData getJoinedChain(ClientRequestInfo ri) {
    SignedData callChain;
    try {
      Any any = ri.get_slot(mediator().getJoinedChainSlotId());
      if (any.type().kind().value() != TCKind._tk_null) {
        callChain = SignedDataHelper.extract(any);
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
    logger.finest(String.format("[inout] send_pool: %s", ri.operation()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_reply(ClientRequestInfo ri) {
    String operation = ri.operation();
    logger.finest(String.format("[in] receive_reply: %s", operation));
    if (!isIgnoringThread(ri)) {
      int uniqueId = getRequestUniqueId();
      String login = clearRequestUniqueId(uniqueId);
      logger.fine(String.format(
        "requisi��o atendida com sucesso! ID (%d) login (%s) opera��o (%s)",
        uniqueId, login, ri.operation()));
    }
    logger.finest(String.format("[out] receive_reply: %s", operation));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    String operation = ri.operation();
    logger.finest(String.format("[in] receive_exception: %s", operation));
    Integer uniqueId = null;
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
          EffectiveProfile ep = new EffectiveProfile(ri.effective_profile());
          ServiceContext context =
            ri.get_reply_service_context(CredentialContextId.value);
          if (context.context_data == null) {
            // n�o recebeu o credential reset
            String message =
              "Servidor chamado � inv�lido (servidor n�o enviou o CredentialReset para negociar sess�o)";
            logger.info(message);
            throw new NO_PERMISSION(message, InvalidRemoteCode.value,
              CompletionStatus.COMPLETED_NO);
          }
          Any credentialResetAny = null;
          try {
            credentialResetAny =
              codec().decode_value(context.context_data,
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
          logger.finest(String.format("Associando entidade '%s'a sess�o '%s'",
            target, reset.session));
          logger.finest(String.format("ForwardRequest ap�s reset: %s", ri
            .operation()));
          throw new ForwardRequest(ri.target());

        case InvalidLoginCode.value:
          LoginInfo info = conn.login();
          if (info != null && loginId.equals(info.id)) {
            conn.localLogout(true);
          }
          logger.info(String.format("Recebeu exce��o InvalidLogin: %s", ri
            .operation()));
          // tenta refazer o login.
          LoginInfo afterLogin = conn.getLogin();
          if (afterLogin == null) {
            throw new NO_PERMISSION("Callback n�o refez o login da conex�o.",
              NoLoginCode.value, CompletionStatus.COMPLETED_NO);
          }

          // callback conseguiu refazer o login
          logger.finest(String.format("ForwardRequest ap�s callback: %s", ri
            .operation()));
          throw new ForwardRequest(ri.target());

        case NoLoginCode.value:
        case UnavailableBusCode.value:
        case InvalidTargetCode.value:
        case InvalidRemoteCode.value:
          String message =
            "Servidor chamado repassou uma exce��o NO_PERMISSION local: minor = %d";
          String msg = String.format(message, exception.minor);
          logger.info(msg);
          throw new NO_PERMISSION(msg, InvalidRemoteCode.value,
            CompletionStatus.COMPLETED_NO);

        case NoCredentialCode.value:
          // deixa a exce��o passar
          break;

        default:
          // deixa a exce��o passar
          break;
      }
    }
    finally {
      if (uniqueId != null) {
        String login = clearRequestUniqueId(uniqueId);
        logger.finest(String.format(
          "Descartando associa��o do ID '%d' com o login '%s'. opera��o (%s)",
          uniqueId, login, ri.operation()));
      }
      logger.finest(String.format("[out] receive_exception: %s", operation));
    }
  }

  /**
   * Recupera o identificador �nico associado ao request.
   * 
   * @return o identificador associado.
   */
  private int getRequestUniqueId() {
    try {
      Current current = ORBUtils.getPICurrent(orb());
      Any uniqueAny = current.get_slot(this.mediator().getUniqueIdSlot());
      if (uniqueAny.type().kind().value() != TCKind._tk_null) {
        return uniqueAny.extract_long();
      }
      else {
        String message = "Any de chave �nica de requestId est� vazia!";
        logger.log(Level.SEVERE, message);
        throw new INTERNAL(message);
      }
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot do request Id";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * Libera os recursos associados ao identificador.
   * 
   * @param uniqueId identificador
   * @return retorna o identificador de login associado ao identificador.
   */
  private String clearRequestUniqueId(int uniqueId) {
    uniqueId2Conn.remove(uniqueId);
    String removed = uniqueId2LoginId.remove(uniqueId);
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
    return removed;
  }

  /**
   * Recupera a conex�o corrente.
   * 
   * @param ri informa��o do request
   * @return a conex�o.
   */
  protected Connection getCurrentConnection(RequestInfo ri) {
    OpenBusContextImpl context = this.mediator().getContext();
    Any any;
    try {
      any = ri.get_slot(context.getCurrentConnectionSlotId());
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot da conex�o corrente";
      throw new INTERNAL(message);
    }
    if (any.type().kind().value() != TCKind._tk_null) {
      int id = any.extract_long();
      Connection connection = context.getConnectionById(id);
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
    logger.finest(String.format("[inout] receive_other: %s", ri.operation()));
  }

}
