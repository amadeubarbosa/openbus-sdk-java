package tecgraf.openbus.core;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.Session.ServerSideSession;
import tecgraf.openbus.core.v2_00.OctetSeqHolder;
import tecgraf.openbus.core.v2_00.credential.CredentialContextId;
import tecgraf.openbus.core.v2_00.credential.CredentialData;
import tecgraf.openbus.core.v2_00.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_00.credential.CredentialReset;
import tecgraf.openbus.core.v2_00.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.CallChain;
import tecgraf.openbus.core.v2_00.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidChainCode;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidPublicKeyCode;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChain;
import tecgraf.openbus.core.v2_00.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_00.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.LRUCache;

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

  /** Cache de sessão: mapa de cliente alvo da chamada para sessão */
  private Map<Integer, ServerSideSession> sessions;
  /** Cache de login */
  private LoginCache loginsCache;

  /**
   * Construtor.
   * 
   * @param name nome do interceptador
   * @param mediator o mediador do ORB
   */
  ServerRequestInterceptorImpl(String name, ORBMediator mediator) {
    super(name, mediator);
    this.sessions =
      Collections.synchronizedMap(new LRUCache<Integer, ServerSideSession>(
        CACHE_SIZE));
    this.loginsCache = new LoginCache(CACHE_SIZE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request_service_contexts(ServerRequestInfo ri) {
    ServiceContext requestServiceContext =
      ri.get_request_service_context(CredentialContextId.value);
    byte[] encodedCredential = requestServiceContext.context_data;
    Any any;
    try {
      any =
        this.getMediator().getCodec().decode_value(encodedCredential,
          CredentialDataHelper.type());
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
    try {
      ri.set_slot(this.getMediator().getCredentialSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao armazenar a credencial em seu slot";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request(ServerRequestInfo ri) {
    String operation = ri.operation();
    BusORB orb = this.getMediator().getORB();
    ConnectionMultiplexerImpl multiplexer =
      this.getMediator().getConnectionMultiplexer();
    try {
      Any credentialDataAny =
        ri.get_slot(this.getMediator().getCredentialSlotId());
      CredentialData credential =
        CredentialDataHelper.extract(credentialDataAny);
      if (credential != null) {
        if (!multiplexer.hasBus(credential.bus)) {
          throw new NO_PERMISSION(UnknownBusCode.value,
            CompletionStatus.COMPLETED_NO);
        }
        ConnectionImpl conn = null;
        if (multiplexer.isMultiplexed()) {
          // se em modo Multiplexed, então deve buscar a conexão default. 
          conn =
            (ConnectionImpl) multiplexer.getIncommingConnection(credential.bus);
          setCurrentConnection(ri, conn);
        }
        if (conn == null) {
          conn = (ConnectionImpl) this.getCurrentConnection(ri);
        }
        String loginId = credential.login;
        if (!loginsCache.validateLogin(loginId, conn)) {
          throw new NO_PERMISSION(InvalidLoginCode.value,
            CompletionStatus.COMPLETED_NO);
        }
        OctetSeqHolder pubkey = new OctetSeqHolder();
        String entity = loginsCache.getLoginEntity(loginId, pubkey, conn);
        if (validateCredential(credential, ri)) {
          if (validateChain(credential.chain, loginId, conn)) {
            // salvando informação da conexão que atendeu a requisição
            Any any = orb.getORB().create_any();
            any.insert_string(conn.login().id);
            ri.set_slot(this.getMediator().getConnectionSlotId(), any);
            String msg =
              "Recebendo chamada pelo barramento: login (%s) entidade (%s) operação (%s)";
            logger.info(String.format(msg, loginId, entity, operation));
          }
          else {
            logger.finest(String.format(
              "Recebeu chamada com cadeia inválida: %s", operation));
            throw new NO_PERMISSION(InvalidChainCode.value,
              CompletionStatus.COMPLETED_NO);
          }
        }
        else {
          logger.finest(String.format(
            "Recebeu chamada sem sessão associda: %s", operation));
          // credencial não é válida. Resetando a credencial da sessão.
          doResetCredential(ri, orb, conn, pubkey.value);
          throw new NO_PERMISSION(InvalidCredentialCode.value,
            CompletionStatus.COMPLETED_NO);
        }
      }
      else {
        logger.info(String.format("Recebeu chamada fora do barramento: %s",
          operation));
      }
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    catch (InvalidLogins e) {
      throw new NO_PERMISSION(InvalidLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (ServiceFailure e) {
      throw new NO_PERMISSION(UnverifiedLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (CryptographyException e) {
      throw new NO_PERMISSION(InvalidPublicKeyCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    finally {
      if (multiplexer.isMultiplexed()) {
        removeCurrentConnection(ri);
      }
    }

  }

  /**
   * Realiza o protocolo de reiniciar a credencial da sessão.
   * 
   * @param ri informação do request.
   * @param orb o orb.
   * @param conn a conexão.
   * @param publicKey a chave pública do requisitante.
   * @throws CryptographyException
   */
  private void doResetCredential(ServerRequestInfo ri, BusORB orb,
    ConnectionImpl conn, byte[] publicKey) throws CryptographyException {
    byte[] newSecret = newSecret();
    Cryptography crypto = Cryptography.getInstance();
    byte[] encriptedSecret =
      crypto.encrypt(newSecret, crypto
        .generateRSAPublicKeyFromX509EncodedKey(publicKey));
    int sessionId = nextAvailableSessionId();
    ServerSideSession newSession =
      new ServerSideSession(sessionId, newSecret, conn.login().id);
    sessions.put(newSession.getSession(), newSession);
    CredentialReset reset =
      new CredentialReset(conn.login().id, sessionId, encriptedSecret);
    Any any = orb.getORB().create_any();
    CredentialResetHelper.insert(any, reset);
    byte[] encodedCredential;
    try {
      encodedCredential = this.getMediator().getCodec().encode_value(any);
    }
    catch (InvalidTypeForEncoding e) {
      String message = "Falha inesperada ao codificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    logger.finest("Resetando a credencial: " + ri.operation());
    ServiceContext requestServiceContext =
      new ServiceContext(CredentialContextId.value, encodedCredential);
    ri.add_reply_service_context(requestServiceContext, false);
  }

  /**
   * Verifica se a credencial é válida, recalculando o hash da mesma.
   * 
   * @param credential a credencial
   * @param ri informação do request.
   * @return <code>true</code> caso a credencial seja válida, ou
   *         <code>false</code> caso contrário.
   */
  private boolean validateCredential(CredentialData credential,
    ServerRequestInfo ri) {
    if (credential.hash == null) {
      // credencial OpenBus 1.5
      logger.finest("Credencial OpenBus 1.5");
      return true;
    }
    ServerSideSession session = sessions.get(credential.session);
    if (session != null) {
      logger.finest(String.format("sessão utilizada: id = %d ticket = %d",
        session.getSession(), credential.ticket));
      byte[] hash =
        this.generateCredentialDataHash(ri, session.getSecret(),
          credential.ticket);
      if (Arrays.equals(hash, credential.hash)
        && session.getTicket().check(credential.ticket)) {
        return true;
      }
      else {
        logger.finest("Falha na validação do hash da credencial");
      }
    }
    return false;
  }

  /**
   * Valida a cadeia da credencial.
   * 
   * @param chain a cadeia.
   * @param caller identificador de login do requisitante da operação.
   * @param conn a conexão em uso.
   * @return <code>true</code> caso a cadeia seja válida, ou <code>false</code>
   *         caso contrário.
   */
  private boolean validateChain(SignedCallChain chain, String caller,
    ConnectionImpl conn) {
    Cryptography crypto = Cryptography.getInstance();
    RSAPublicKey busPubKey = conn.getBusPublicKey();
    if (chain != null) {
      if (chain.signature != null) {
        try {
          Any any =
            this.getMediator().getCodec().decode_value(chain.encoded,
              CallChainHelper.type());
          CallChain callChain = CallChainHelper.extract(any);
          boolean verified =
            crypto.verifySignature(busPubKey, chain.encoded, chain.signature);
          if (verified && callChain.target.equals(conn.login().id)) {
            LoginInfo[] callers = callChain.callers;
            if (callers[callers.length - 1].id.equals(caller)) {
              return true;
            }
          }
        }
        catch (CryptographyException e) {
          String message =
            "Falha inesperada ao verificar assinatura da cadeia.";
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message);
        }
        catch (FormatMismatch e) {
          String message = "Falha inesperada ao decodificar a cadeia";
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message);
        }
        catch (TypeMismatch e) {
          String message = "Falha inesperada ao decodificar a cadeia";
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message);
        }
      }
      else {
        // TODO: cadeia openbus 1.5 ?
        logger.finest("Cadeia OpenBus 1.5");
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_reply(ServerRequestInfo ri) {
    Any any = this.getMediator().getORB().getORB().create_any();
    try {
      ri.set_slot(this.getMediator().getCredentialSlotId(), any);
      ri.set_slot(this.getMediator().getConnectionSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao limpar informações nos slots";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_exception(ServerRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_other(ServerRequestInfo ri) {
  }

  /**
   * Recupera o próximo indentificador de sessão disponível.
   * 
   * @return o Identificador de sessão.
   */
  private int nextAvailableSessionId() {
    synchronized (sessions) {
      for (int i = 1; i <= CACHE_SIZE + 1; i++) {
        if (!sessions.containsKey(i)) {
          return i;
        }
      }
    }
    // não deveria chegar neste ponto
    return CACHE_SIZE + 1;
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
  private void setCurrentConnection(ServerRequestInfo ri, Connection conn) {
    long id = Thread.currentThread().getId();
    ConnectionMultiplexerImpl multiplexer =
      this.getMediator().getConnectionMultiplexer();
    Any any = this.getMediator().getORB().getORB().create_any();
    any.insert_longlong(id);
    try {
      ri.set_slot(multiplexer.getCurrentThreadSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    multiplexer.setConnectionByThreadId(id, conn);
  }

  /**
   * Remove a conexão corrente do slot de ThreadId
   * 
   * @param ri a informação do request.
   */
  private void removeCurrentConnection(ServerRequestInfo ri) {
    long id = Thread.currentThread().getId();
    ConnectionMultiplexerImpl multiplexer =
      this.getMediator().getConnectionMultiplexer();
    Any any = this.getMediator().getORB().getORB().create_any();
    try {
      ri.set_slot(multiplexer.getCurrentThreadSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    multiplexer.setConnectionByThreadId(id, null);
  }

}
