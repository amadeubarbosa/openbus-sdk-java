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
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UserException;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.core.Credential.Chain;
import tecgraf.openbus.core.Credential.Reset;
import tecgraf.openbus.core.Session.ServerSideSession;
import tecgraf.openbus.core.v2_1.OctetSeqHolder;
import tecgraf.openbus.core.v2_1.credential.CredentialContextId;
import tecgraf.openbus.core.v2_1.credential.CredentialData;
import tecgraf.openbus.core.v2_1.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidChainCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidPublicKeyCode;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.NoCredentialCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.interceptors.CallChainInfo;
import tecgraf.openbus.interceptors.CallChainInfoHelper;
import tecgraf.openbus.security.Cryptography;

/**
 * Interceptador servidor.
 * 
 * @author Tecgraf
 */
final class ServerRequestInterceptorImpl extends InterceptorImpl implements
  ServerRequestInterceptor {

  /** Instância de logging. */
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

  @Override
  public void receive_request_service_contexts(ServerRequestInfo ri)
    throws ForwardRequest {
    // do nothing
  }

  /**
   * Recupera a credencial do contexto.
   * 
   * @param ri informação do contexto
   * @return a credencial.
   */
  private Credential retrieveCredential(ServerRequestInfo ri) {
    byte[] encodedCredential;
    try {
      ServiceContext requestServiceContext =
        ri.get_request_service_context(CredentialContextId.value);
      encodedCredential = requestServiceContext.context_data;
      if (encodedCredential != null) {
        Any any =
          codec().decode_value(encodedCredential, CredentialDataHelper.type());
        CredentialData credential = CredentialDataHelper.extract(any);
        return new Credential(credential);
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
    catch (TypeMismatch | FormatMismatch e) {
      String message = "Falha inesperada ao decodificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

    // suporte legado
    try {
      ServiceContext requestServiceContext =
        ri.get_request_service_context(tecgraf.openbus.core.v2_0.credential.CredentialContextId.value);
      encodedCredential = requestServiceContext.context_data;
      if (encodedCredential != null) {
        Any any =
          codec().decode_value(encodedCredential,
            tecgraf.openbus.core.v2_0.credential.CredentialDataHelper.type());
        tecgraf.openbus.core.v2_0.credential.CredentialData credential =
          tecgraf.openbus.core.v2_0.credential.CredentialDataHelper
            .extract(any);
        return new Credential(credential);
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
    catch (TypeMismatch | FormatMismatch e) {
      String message = "Falha inesperada ao decodificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

    String message = "Nenhuma credencial suportada encontrada";
    logger.info(message);
    throw new NO_PERMISSION(message, NoCredentialCode.value,
      CompletionStatus.COMPLETED_NO);
  }

  @Override
  public void receive_request(ServerRequestInfo ri) {
    String operation = ri.operation();
    logger.finest(String.format("[in] receive_request: %s", operation));
    int requestId = ri.request_id();
    byte[] object_id = ri.object_id();
    OpenBusContextImpl context = context();
    Credential credential = retrieveCredential(ri);
    try {
      String busId = credential.bus;
      String loginId = credential.login;
      ConnectionImpl conn =
        getConnForDispatch(context, busId, loginId, object_id, operation);
      if (busId.equals(conn.busId())) {
        context.currentConnection(conn);
        if (validateLogin(conn, loginId, ri)) {
          OctetSeqHolder pubkey = new OctetSeqHolder();
          String entity = getLoginInfo(conn, loginId, pubkey, ri);
          if (validateCredential(credential, ri, conn)) {
            if (validateChain(credential, null, conn)) {
              saveRequestInformations(credential, conn, ri);
              String msg =
                "Recebendo chamada pelo barramento: login (%s) entidade (%s) operação (%s) requestId (%d)";
              logger.fine(String.format(msg, loginId, entity, operation,
                requestId));
              return;
            }
            else {
              logger
                .fine(String
                  .format(
                    "Recebeu chamada com cadeia inválida: operação (%s) requestId (%d)",
                    operation, requestId));
              throw new NO_PERMISSION(InvalidChainCode.value,
                CompletionStatus.COMPLETED_NO);
            }
          }
          else {
            // credencial não é válida. Resetando a credencial da sessão.
            doResetCredential(ri, conn, credential, pubkey.value);
            throw new NO_PERMISSION(InvalidCredentialCode.value,
              CompletionStatus.COMPLETED_NO);
          }
        }
        else {
          logger.severe(String.format(
            "Credencial com login inválido: operação (%s) login (%s)",
            operation, loginId));
          throw new NO_PERMISSION(InvalidLoginCode.value,
            CompletionStatus.COMPLETED_NO);
        }
      }
      else {
        logger
          .severe(String
            .format(
              "Recebeu chamade de outro barramento: operação (%s) login (%s) bus (%s)",
              operation, loginId, busId));
        throw new NO_PERMISSION(InvalidLoginCode.value,
          CompletionStatus.COMPLETED_NO);
      }
    }
    catch (CryptographyException e) {
      String message = "Falha ao criptografar com chave pública";
      logger.log(Level.SEVERE, message, e);
      throw new NO_PERMISSION(InvalidPublicKeyCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    finally {
      // Talvez essa operação nao deveria ser necessaria, 
      // pois o PICurrent deveria acabar junto com a thread de interceptação.
      // CHECK possível bug! Esta operação modifica o valor setado no ri e PICurrent
      //context.currentConnection(null);
      logger.finest(String.format("[out] receive_request: %s", operation));
    }
  }

  /**
   * Recupera a conexão a ser utilizada no dispatch.
   * 
   * @param context Gerenciador de contexto do ORB que recebeu a chamada.
   * @param busId Identificação do barramento através do qual a chamada foi
   *        feita.
   * @param loginId Informações do login que se tornou inválido.
   * @param object_id Idenficador opaco descrevendo o objeto sendo chamado.
   * @param operation Nome da operação sendo chamada.
   * 
   * @return Conexão a ser utilizada para receber a chamada.
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
          "Callback 'onCallDispatch' gerou um erro durante execução.", e);
      }
      //TODO caso callback gere um erro, busco a conexao default ou NO_PERMISSION?
    }
    if (conn == null) {
      conn = (ConnectionImpl) context.defaultConnection();
    }
    if (conn == null || conn.login() == null || !conn.busId().equals(busId)) {
      throw new NO_PERMISSION(UnknownBusCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    return conn;
  }

  /**
   * Realiza o protocolo de reiniciar a credencial da sessão.
   * 
   * @param ri informação do request.
   * @param conn a conexão.
   * @param credential a credencial associada a chamada.
   * @param publicKey a chave pública do requisitante.
   * @throws CryptographyException
   */
  private void doResetCredential(ServerRequestInfo ri, ConnectionImpl conn,
    Credential credential, byte[] publicKey) throws CryptographyException {
    byte[] newSecret = newSecret();
    Cryptography crypto = Cryptography.getInstance();
    byte[] encriptedSecret =
      crypto.encrypt(newSecret, crypto
        .generateRSAPublicKeyFromX509EncodedKey(publicKey));
    int sessionId = conn.nextAvailableSessionId();
    ServerSideSession newSession =
      new ServerSideSession(sessionId, newSecret, credential.login);
    conn.cache.srvSessions.put(newSession.getSession(), newSession);
    LoginInfo login = conn.login();
    Reset reset =
      new Reset(login, sessionId, encriptedSecret, credential.legacy);
    try {
      ri.add_reply_service_context(reset.toServiceContext(orb(), codec()),
        false);
      logger.fine(String.format(
        "Resetando a credencial: operação (%s) requestId (%d)", ri.operation(),
        ri.request_id()));
    }
    catch (InvalidTypeForEncoding e) {
      String message =
        String.format("Falha ao codificar reset: operação (%s) requestId (%d)",
          ri.operation(), ri.request_id());
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * Verifica a validade do login.
   * 
   * @param conn conexão em uso
   * @param loginId identificador do login
   * @param ri informações da requisição
   * @return {@code true} caso o login seja válido, ou {@code false}
   *         caso contrário.
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
          "Erro ao validar o login. Conexão dispatcher está deslogada. operação (%s) requestId (%d)";
        logger.log(Level.SEVERE, String.format(message, operation, requestId),
          e);
        throw new NO_PERMISSION(UnknownBusCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      else {
        String message =
          "Erro ao validar o login. operação (%s) requestId (%d)";
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
   * Recupera informações do login
   * 
   * @param conn conexão em uso
   * @param loginId identifador do login de interesse
   * @param pubkey holder da chave pública do login
   * @param ri informações da requisição
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
        "Login verificado é inválido. operação (%s) requestId (%d)";
      logger.log(Level.SEVERE, String.format(message, operation, requestId), e);
      throw new NO_PERMISSION(InvalidLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        String message =
          "Erro ao verificar o login. Conexão dispatcher está deslogada. operação (%s) requestId (%d)";
        logger.log(Level.SEVERE, String.format(message, operation, requestId),
          e);
        throw new NO_PERMISSION(UnknownBusCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      else {
        String message =
          "Erro ao verificar o login. operação (%s) requestId (%d)";
        logger.log(Level.SEVERE, String.format(message, operation, requestId),
          e);
        throw new NO_PERMISSION(UnverifiedLoginCode.value,
          CompletionStatus.COMPLETED_NO);
      }
    }
    catch (Exception e) {
      String message =
        "Erro ao verificar o login. operação (%s) requestId (%d)";
      logger.log(Level.SEVERE, String.format(message, operation, requestId), e);
      throw new NO_PERMISSION(UnverifiedLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * Verifica se a credencial é válida, recalculando o hash da mesma.
   * 
   * @param credential a credencial
   * @param ri informação do request.
   * @param conn a conexão em uso.
   * @return {@code true} caso a credencial seja válida, ou {@code false}
   * caso contrário.
   */
  private boolean validateCredential(Credential credential,
    ServerRequestInfo ri, ConnectionImpl conn) {
    ServerSideSession session = conn.cache.srvSessions.get(credential.session);
    if (session != null && session.getCaller().equals(credential.login)) {
      byte[] hash =
        this.generateCredentialDataHash(ri, session.getSecret(),
          credential.ticket, credential.legacy);
      if (Arrays.equals(hash, credential.hash)
        && session.checkTicket(credential.ticket)) {
        logger
          .finest(String
            .format(
              "credencial válida: operação (%s) requestId (%d) sessão (%d) ticket (%d)",
              ri.operation(), ri.request_id(), session.getSession(),
              credential.ticket));
        return true;
      }
      else {
        logger
          .finest(String
            .format(
              "Falha na validação do hash da credencial: operação (%s) requestId (%d)",
              ri.operation(), ri.request_id()));
      }
    }
    else {
      logger.fine(String.format(
        "Recebeu chamada sem sessão associda: operação (%s) requestId (%d)", ri
          .operation(), ri.request_id()));
    }
    return false;
  }

  /**
   * Valida a cadeia de uma credencial.
   * 
   * @param credential a credencial
   * @param pubKey a chave pública da entidade. Caso seja null, a assinatura
   *               será validada com a chave pública do barramento.
   * @param conn a conexão em uso.
   * @return {@code true} caso a cadeia seja válida, ou {@code false} caso
   * contrário.
   */
  private boolean validateChain(Credential credential, RSAPublicKey pubKey,
    ConnectionImpl conn) {
    Cryptography crypto = Cryptography.getInstance();
    if (pubKey == null) {
      pubKey = conn.busPublicKey();
    }
    if (credential.chain != null) {
      try {
        Chain chain = credential.decodeChain(codec());
        boolean verified =
          crypto.verifySignature(pubKey, chain.encoded(), chain.signature());
        if (verified && (chain.bus.equals(credential.bus))
          && (chain.target.equals(conn.login().entity))
          && (chain.caller.id.equals(credential.login))) {

          return true;
        }
      }
      catch (UserException e) {
        String message = "Falha inesperada ao decodificar a cadeia";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
      catch (CryptographyException e) {
        String message = "Falha inesperada ao verificar assinatura da cadeia.";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
    }
    return false;
  }

  /**
   * Salva as informações associadas à requisição
   * 
   * @param credential a credencial
   * @param conn conexão utilizada no atendimento da requisição
   * @param ri as informações da requisição
   */
  private void saveRequestInformations(Credential credential,
    ConnectionImpl conn, ServerRequestInfo ri) {
    // salvando a cadeia
    CallChainInfo chainInfo = new CallChainInfo();
    Chain chain = credential.chain;
    chainInfo.chain = chain.signedChain;
    chainInfo.legacy = credential.legacy;
    chainInfo.bus = credential.bus;
    chainInfo.legacy_chain = chain.signedLegacy;
    if (!credential.legacy && conn.legacy()
      && (conn.legacySupport().converter() != null)) {
      CallChain callchain =
        new CallChain(chain.bus, chain.target, chain.originators, chain.caller);
      try {
        context().joinChain(new CallerChainImpl(callchain, chain.signedChain));
        chainInfo.legacy_chain =
          conn.legacySupport().converter().convertSignedChain();
      }
      catch (Exception e) {
        String err =
          String.format(
            "Falha ao converter cadeia assinada: operação (%s) requestId (%d)",
            ri.operation(), ri.request_id());
        logger.log(Level.SEVERE, err, e);
        throw new NO_PERMISSION(err, NoCredentialCode.value,
          CompletionStatus.COMPLETED_NO);
      }
      finally {
        context().exitChain();
      }
    }
    Any any = orb().create_any();
    CallChainInfoHelper.insert(any, chainInfo);
    try {
      ri.set_slot(mediator().getSignedChainSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao armazenar dados em slot";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    // salva a conexão utilizada no dispatch
    // CHECK se o bug relatado no finally for verdadeiro, isso pode ser desnecessario
    setCurrentConnection(ri, conn);
  }

  @Override
  public void send_reply(ServerRequestInfo ri) {
    String operation = ri.operation();
    // CHECK deveria setar o currentConnection antigo?
    removeCurrentConnection(ri);

    // CHECK verificar se preciso limpar mais algum slot
    Any any = orb().create_any();
    try {
      ri.set_slot(mediator().getSignedChainSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao limpar informações nos slots";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    String msg = "Chamada atendida: operação (%s) requestId (%d)";
    logger.fine(String.format(msg, operation, ri.request_id()));
  }

  @Override
  public void send_exception(ServerRequestInfo ri) {
  }

  @Override
  public void send_other(ServerRequestInfo ri) {
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
   * Configura a conexão corrente desta requisição.
   * 
   * @param ri informação da requisição
   * @param conn a conexão corrente.
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
            "Salvando conexão que realiza o dispatch: conexão (%s) login (%s) operação (%s) requestId (%s)",
            conn.connId(), conn.login().id, ri.operation(), ri.request_id()));
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * Remove a conexão corrente do slot de ThreadId
   * 
   * @param ri a informação do request.
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
          "BUG: Falha inesperada ao acessar o slot da conexão corrente";
        logger.log(Level.SEVERE, message);
        throw new INTERNAL(message);
      }

      // limpando informação da conexão
      // TODO Dúvida:
      /*
       * o orb garante que não modificou a informação no PICurrent da thread que
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
