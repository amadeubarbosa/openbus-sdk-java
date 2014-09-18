package tecgraf.openbus.core;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.core.Session.ServerSideSession;
import tecgraf.openbus.core.v2_1.OctetSeqHolder;
import tecgraf.openbus.core.v2_1.credential.CredentialContextId;
import tecgraf.openbus.core.v2_1.credential.CredentialData;
import tecgraf.openbus.core.v2_1.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_1.credential.CredentialReset;
import tecgraf.openbus.core.v2_1.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.credential.SignedDataHelper;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidChainCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidPublicKey;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidPublicKeyCode;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.NoCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.security.Cryptography;

/**
 * Interceptador servidor.
 * 
 * @author Tecgraf
 */
final class ServerRequestInterceptorImpl extends InterceptorImpl implements
  ServerRequestInterceptor {

  /** Inst�ncia de logging. */
  private static final Logger logger = Logger
    .getLogger(ServerRequestInterceptorImpl.class.getName());

  /**
   * Construtor.
   * 
   * @param name nome do interceptador
   * @param mediator o mediador do ORB
   */
  ServerRequestInterceptorImpl(String name, ORBMediator mediator) {
    super(name, mediator);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request_service_contexts(ServerRequestInfo ri)
    throws ForwardRequest {
    logger.finest(String.format("[inout] receive_request_service_contexts: %s",
      ri.operation()));
    // do nothing
  }

  /**
   * Recupera a credencial do contexto.
   * 
   * @param ri informa��o do contexto
   * @return a credencial.
   */
  private CredentialData retrieveCredential(ServerRequestInfo ri) {
    byte[] encodedCredential = null;
    try {
      ServiceContext requestServiceContext =
        ri.get_request_service_context(CredentialContextId.value);
      encodedCredential = requestServiceContext.context_data;
      if (encodedCredential != null) {
        Any any =
          codec().decode_value(encodedCredential, CredentialDataHelper.type());
        CredentialData credential = CredentialDataHelper.extract(any);
        return credential;
      }
    }
    catch (BAD_PARAM e) {
      switch (e.minor) {
        case 26:
          break;
        default:
          throw e;
      }
    }
    catch (TypeMismatch e) {
      String message = "Falha inesperada ao decodificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    catch (FormatMismatch e) {
      String message = "Falha inesperada ao decodificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    String message = "Nenhuma credencial suportada encontrada";
    logger.info(message);
    throw new NO_PERMISSION(message, NoCredentialCode.value,
      CompletionStatus.COMPLETED_NO);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request(ServerRequestInfo ri) {
    String operation = ri.operation();
    logger.finest(String.format("[in] receive_request: %s", operation));
    int requestId = ri.request_id();
    byte[] object_id = ri.object_id();
    OpenBusContextImpl context = mediator().getContext();
    CredentialData credential = retrieveCredential(ri);
    try {
      String busId = credential.bus;
      String loginId = credential.login;
      ConnectionImpl conn =
        getConnForDispatch(context, busId, loginId, object_id, operation);
      context.setCurrentConnection(conn);
      if (validateLogin(conn, loginId, ri)) {
        OctetSeqHolder pubkey = new OctetSeqHolder();
        String entity = getLoginInfo(conn, loginId, pubkey, ri);
        if (validateCredential(credential, ri, conn)) {
          if (validateChain(credential, pubkey, ri, conn)) {
            // salvando informa��o do barramento que atendeu a requisi��o
            ORB orb = mediator().getORB();
            Any any = orb.create_any();
            any.insert_string(conn.busid());
            ri.set_slot(mediator().getBusSlotId(), any);
            String msg =
              "Recebendo chamada pelo barramento: login (%s) entidade (%s) opera��o (%s) requestId (%d)";
            logger.fine(String.format(msg, loginId, entity, operation,
              requestId));
            // salva a conex�o utilizada no dispatch
            // CHECK se o bug relatado no finally for verdadeiro, isso pode ser desnecessario
            setCurrentConnection(ri, conn);
            return;
          }
          else {
            logger
              .fine(String
                .format(
                  "Recebeu chamada com cadeia inv�lida: opera��o (%s) requestId (%d)",
                  operation, requestId));
            throw new NO_PERMISSION(InvalidChainCode.value,
              CompletionStatus.COMPLETED_NO);
          }
        }
        else {
          logger
            .fine(String
              .format(
                "Recebeu chamada sem sess�o associda: opera��o (%s) requestId (%d)",
                operation, requestId));
          // credencial n�o � v�lida. Resetando a credencial da sess�o.
          doResetCredential(ri, conn, loginId, pubkey.value);
          throw new NO_PERMISSION(InvalidCredentialCode.value,
            CompletionStatus.COMPLETED_NO);
        }
      }
      else {
        throw new NO_PERMISSION(InvalidLoginCode.value,
          CompletionStatus.COMPLETED_NO);
      }
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    catch (CryptographyException e) {
      String message = "Falha ao criptografar com chave p�blica";
      logger.log(Level.SEVERE, message, e);
      throw new NO_PERMISSION(InvalidPublicKeyCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    finally {
      // Talvez essa opera��o nao deveria ser necessaria, 
      // pois o PICurrent deveria acabar junto com a thread de intercepta��o.
      // CHECK poss�vel bug! Esta opera��o modifica o valor setado no ri e PICurrent
      //context.setCurrentConnection(null);
      logger.finest(String.format("[out] receive_request: %s", operation));
    }
  }

  /**
   * Recupera a conex�o a ser utilizada no dispatch.
   * 
   * @param context Gerenciador de contexto do ORB que recebeu a chamada.
   * @param busId Identifica��o do barramento atrav�s do qual a chamada foi
   *        feita.
   * @param loginId Informa��es do login que se tornou inv�lido.
   * @param object_id Idenficador opaco descrevendo o objeto sendo chamado.
   * @param operation Nome da opera��o sendo chamada.
   * 
   * @return Conex�o a ser utilizada para receber a chamada.
   */
  private ConnectionImpl getConnForDispatch(OpenBusContextImpl context,
    String busId, String loginId, byte[] object_id, String operation) {
    ConnectionImpl conn = null;
    CallDispatchCallback onCallDispatch = context.onCallDispatch();
    if (onCallDispatch != null) {
      try {
        conn =
          (ConnectionImpl) onCallDispatch.dispatch(context, busId, loginId,
            object_id, operation);
      }
      catch (Exception e) {
        logger.log(Level.SEVERE,
          "Callback 'onCallDispatch' gerou um erro durante execu��o.", e);
      }
      //TODO caso callback gere um erro, busco a conexao default ou NO_PERMISSION?
    }
    if (conn == null) {
      conn = (ConnectionImpl) context.getDefaultConnection();
      if (conn == null) {
        throw new NO_PERMISSION(UnknownBusCode.value,
          CompletionStatus.COMPLETED_NO);
      }
    }
    if (conn.login() == null || !conn.busid().equals(busId)) {
      throw new NO_PERMISSION(UnknownBusCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    return conn;
  }

  /**
   * Realiza o protocolo de reiniciar a credencial da sess�o.
   * 
   * @param ri informa��o do request.
   * @param conn a conex�o.
   * @param caller identificador do login originador da chamada.
   * @param publicKey a chave p�blica do requisitante.
   * @throws CryptographyException
   */
  private void doResetCredential(ServerRequestInfo ri, ConnectionImpl conn,
    String caller, byte[] publicKey) throws CryptographyException {
    byte[] newSecret = newSecret();
    Cryptography crypto = Cryptography.getInstance();
    byte[] encriptedSecret =
      crypto.encrypt(newSecret, crypto
        .generateRSAPublicKeyFromX509EncodedKey(publicKey));
    int sessionId = conn.nextAvailableSessionId();
    ServerSideSession newSession =
      new ServerSideSession(sessionId, newSecret, caller);
    conn.cache.srvSessions.put(newSession.getSession(), newSession);
    CredentialReset reset =
      new CredentialReset(conn.login().id, sessionId, encriptedSecret);
    Any any = orb().create_any();
    CredentialResetHelper.insert(any, reset);
    byte[] encodedCredential;
    try {
      encodedCredential = codec().encode_value(any);
    }
    catch (InvalidTypeForEncoding e) {
      String message =
        String
          .format(
            "Falha inesperada ao codificar a credencial: opera��o (%s) requestId (%d)",
            ri.operation(), ri.request_id());
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    logger.fine(String.format(
      "Resetando a credencial: opera��o (%s) requestId (%d)", ri.operation(),
      ri.request_id()));
    ServiceContext requestServiceContext =
      new ServiceContext(CredentialContextId.value, encodedCredential);
    ri.add_reply_service_context(requestServiceContext, false);
  }

  /**
   * Verifica a validade do login.
   * 
   * @param conn conex�o em uso
   * @param loginId identificador do login
   * @param ri informa��es da requisi��o
   * @return <code>true</code> caso o login seja v�lido, ou <code>false</code>
   *         caso contr�rio.
   */
  private boolean validateLogin(ConnectionImpl conn, String loginId,
    ServerRequestInfo ri) {
    try {
      return conn.cache.logins.validateLogin(loginId);
    }
    catch (NO_PERMISSION e) {
      String operation = ri.operation();
      int requestId = ri.request_id();
      if (e.minor == NoLoginCode.value) {
        String message =
          "Erro ao validar o login. Conex�o dispatcher est� deslogada. opera��o (%s) requestId (%d)";
        logger.log(Level.SEVERE, String.format(message, operation, requestId),
          e);
        throw new NO_PERMISSION(UnknownBusCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      else {
        String message =
          "Erro ao validar o login. opera��o (%s) requestId (%d)";
        logger.log(Level.SEVERE, String.format(message, operation, requestId),
          e);
        throw new NO_PERMISSION(UnverifiedLoginCode.value,
          CompletionStatus.COMPLETED_NO);
      }
    }
    catch (Exception e) {
      String message = "Erro ao validar o login.";
      logger.log(Level.SEVERE, message, e);
      throw new NO_PERMISSION(UnverifiedLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * Recupera informa��es do login
   * 
   * @param conn conex�o em uso
   * @param loginId identifador do login de interesse
   * @param pubkey holder da chave p�blica do login
   * @param ri informa��es da requisi��o
   * @return o nome da entidade associada ao login
   */
  private String getLoginInfo(ConnectionImpl conn, String loginId,
    OctetSeqHolder pubkey, ServerRequestInfo ri) {
    String operation = ri.operation();
    int requestId = ri.request_id();
    try {
      return conn.cache.logins.getLoginEntity(loginId, pubkey);
    }
    catch (InvalidLogins e) {
      String message =
        "Login verificado � inv�lido. opera��o (%s) requestId (%d)";
      logger.log(Level.SEVERE, String.format(message, operation, requestId), e);
      throw new NO_PERMISSION(InvalidLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (InvalidPublicKey e) {
      String message =
        "Chave p�blica associada ao login � inv�lida. opera��o (%s) requestId (%d)";
      logger.log(Level.SEVERE, String.format(message, operation, requestId), e);
      throw new NO_PERMISSION(InvalidPublicKeyCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (ServiceFailure e) {
      String message =
        "Erro ao verificar o login. opera��o (%s) requestId (%d)";
      logger.log(Level.SEVERE, String.format(message, operation, requestId), e);
      throw new NO_PERMISSION(UnverifiedLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        String message =
          "Erro ao verificar o login. Conex�o dispatcher est� deslogada. opera��o (%s) requestId (%d)";
        logger.log(Level.SEVERE, String.format(message, operation, requestId),
          e);
        throw new NO_PERMISSION(UnknownBusCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      else {
        String message =
          "Erro ao verificar o login. opera��o (%s) requestId (%d)";
        logger.log(Level.SEVERE, String.format(message, operation, requestId),
          e);
        throw new NO_PERMISSION(UnverifiedLoginCode.value,
          CompletionStatus.COMPLETED_NO);
      }
    }
    catch (Exception e) {
      String message =
        "Erro ao verificar o login. opera��o (%s) requestId (%d)";
      logger.log(Level.SEVERE, String.format(message, operation, requestId), e);
      throw new NO_PERMISSION(UnverifiedLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * Verifica se a credencial � v�lida, recalculando o hash da mesma.
   * 
   * @param credential a credencial
   * @param ri informa��o do request.
   * @param conn a conex�o em uso.
   * @return <code>true</code> caso a credencial seja v�lida, ou
   *         <code>false</code> caso contr�rio.
   */
  private boolean validateCredential(CredentialData credential,
    ServerRequestInfo ri, ConnectionImpl conn) {
    ServerSideSession session = conn.cache.srvSessions.get(credential.session);
    if (session != null && session.getCaller().equals(credential.login)) {
      logger.finest(String.format("sess�o utilizada: id = %d ticket = %d",
        session.getSession(), credential.ticket));
      byte[] hash =
        this.generateCredentialDataHash(ri, session.getSecret(),
          credential.ticket);
      if (Arrays.equals(hash, credential.hash)
        && session.checkTicket(credential.ticket)) {
        return true;
      }
      else {
        logger.finest("Falha na valida��o do hash da credencial");
      }
    }
    return false;
  }

  /**
   * Valida a cadeia da credencial.
   * 
   * @param credential a credencial
   * @param pubkey a chave p�blica da entidade
   * @param ri informa��es do request
   * @param conn a conex�o em uso.
   * @return <code>true</code> caso a cadeia seja v�lida, ou <code>false</code>
   *         caso contr�rio.
   */
  private boolean validateChain(CredentialData credential,
    OctetSeqHolder pubkey, ServerRequestInfo ri, ConnectionImpl conn) {
    Cryptography crypto = Cryptography.getInstance();
    RSAPublicKey busPubKey = conn.getBusPublicKey();
    SignedData chain = credential.chain;
    if (chain != null) {
      CallChain callChain = decodeSignedChain(chain, logger);
      try {
        boolean verified =
          crypto.verifySignature(busPubKey, chain.encoded, chain.signature);
        if (verified) {
          LoginInfo loginInfo = conn.login();
          if (callChain.target.equals(loginInfo.entity)) {
            LoginInfo caller = callChain.caller;
            if (caller.id.equals(credential.login)) {
              // salvando a cadeia
              Any singnedAny = orb().create_any();
              SignedDataHelper.insert(singnedAny, chain);
              ri.set_slot(this.mediator().getSignedChainSlotId(), singnedAny);
              return true;
            }
          }
          else {
            logger.fine(String.format("Resetando credencial: opera��o (%s)", ri
              .operation()));
            // credencial n�o � v�lida. Resetando a credencial da sess�o.
            doResetCredential(ri, conn, credential.login, pubkey.value);
            throw new NO_PERMISSION(InvalidCredentialCode.value,
              CompletionStatus.COMPLETED_NO);
          }
        }
      }
      catch (CryptographyException e) {
        String message = "Falha inesperada ao verificar assinatura da cadeia.";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
      catch (InvalidSlot e) {
        String message =
          "Falha inesperada ao armazenar o dados no slot de contexto";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_reply(ServerRequestInfo ri) {
    String operation = ri.operation();
    logger.finest(String.format("[in] send_reply: %s", operation));

    // CHECK deveria setar o currentConnection antigo?
    removeCurrentConnection(ri);

    // CHECK verificar se preciso limpar mais algum slot
    Any any = orb().create_any();
    try {
      ri.set_slot(mediator().getSignedChainSlotId(), any);
      ri.set_slot(mediator().getBusSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao limpar informa��es nos slots";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    String msg = "Chamada atendida: opera��o (%s) requestId (%d)";
    logger.fine(String.format(msg, operation, ri.request_id()));
    logger.finest(String.format("[out] send_reply: %s", operation));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_exception(ServerRequestInfo ri) {
    logger.finest(String.format("[inout] send_exception: %s", ri.operation()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_other(ServerRequestInfo ri) {
    logger.finest(String.format("[inout] send_other: %s", ri.operation()));
  }

  /**
   * Gera um novo segredo.
   * 
   * @return o segredo.
   */
  private byte[] newSecret() {
    int size = 16;
    byte[] secret = new byte[size];
    Random random = new Random();
    random.nextBytes(secret);
    return secret;
  }

  /**
   * Configura a conex�o corrente desta requisi��o.
   * 
   * @param ri informa��o da requisi��o
   * @param conn a conex�o corrente.
   */
  private void setCurrentConnection(ServerRequestInfo ri, ConnectionImpl conn) {
    try {
      int id = mediator().getUniqueId();
      Any any = orb().create_any();
      any.insert_long(id);
      OpenBusContextImpl context = mediator().getContext();
      ri.set_slot(context.getCurrentConnectionSlotId(), any);
      context.setConnectionById(id, conn);
      logger
        .finest(String
          .format(
            "Salvando conex�o que realiza o dispatch: conex�o (%s) login (%s) opera��o (%s) requestId (%s)",
            conn.connId(), conn.login().id, ri.operation(), ri.request_id()));
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * Remove a conex�o corrente do slot de ThreadId
   * 
   * @param ri a informa��o do request.
   */
  private void removeCurrentConnection(ServerRequestInfo ri) {
    try {
      OpenBusContextImpl context = mediator().getContext();
      Any slot = ri.get_slot(context.getCurrentConnectionSlotId());
      if (slot.type().kind().value() != TCKind._tk_null) {
        int id = slot.extract_long();
        context.setConnectionById(id, null);
      }
      else {
        // nunca deveria acontecer.
        String message =
          "BUG: Falha inesperada ao acessar o slot da conex�o corrente";
        logger.log(Level.SEVERE, message);
        throw new INTERNAL(message);
      }

      // limpando informa��o da conex�o
      // TODO D�vida:
      /*
       * o orb garante que n�o modificou a informa��o no PICurrent da thread que
       * originalmente fez um setCurrentConnection?
       */
      Any any = orb().create_any();
      ri.set_slot(context.getCurrentConnectionSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

  }

}
