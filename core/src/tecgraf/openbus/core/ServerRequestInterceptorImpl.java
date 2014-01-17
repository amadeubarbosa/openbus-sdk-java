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
import org.omg.IOP.Codec;
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
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.CredentialHelper;
import tecgraf.openbus.core.v2_0.OctetSeqHolder;
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
import tecgraf.openbus.core.v2_0.services.access_control.InvalidChainCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidCredentialCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidPublicKeyCode;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.NoCredentialCode;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnverifiedLoginCode;
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

  /** Valor de busid desconhecido. */
  private static final String UNKNOWN_BUS = "";

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
  public void receive_request_service_contexts(ServerRequestInfo arg0)
    throws ForwardRequest {
    // do nothing
  }

  /**
   * Recupera a credencial do contexto.
   * 
   * @param ri informa��o do contexto
   * @return Wrapper para a credencial extra�da.
   */
  private CredentialWrapper retrieveCredential(ServerRequestInfo ri) {
    ORB orb = this.getMediator().getORB();
    Codec codec = this.getMediator().getCodec();
    byte[] encodedCredential = null;
    try {
      ServiceContext requestServiceContext =
        ri.get_request_service_context(CredentialContextId.value);
      encodedCredential = requestServiceContext.context_data;
    }
    catch (BAD_PARAM e) {
      // FIXME: BAD_PARAM caso n�o exista service context com o id especificado
      // CORBA ESPEC define com minor code 26.
      // JacORB define com minor code 23.
      switch (e.minor) {
        case 26:
        case 23:
          break;
        default:
          throw e;
      }
    }
    if (encodedCredential != null) {
      Any any;
      try {
        any =
          codec.decode_value(encodedCredential, CredentialDataHelper.type());
        CredentialData credential = CredentialDataHelper.extract(any);
        return new CredentialWrapper(false, credential, null);
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
    }
    else {
      int legacyContextId = 1234;
      byte[] encodedLegacyCredential = null;
      try {
        ServiceContext serviceContext =
          ri.get_request_service_context(legacyContextId);
        encodedLegacyCredential = serviceContext.context_data;
      }
      catch (BAD_PARAM e) {
        // FIXME: BAD_PARAM caso n�o exista service context com o id especificado
        // CORBA ESPEC define com minor code 26.
        // JacORB define com minor code 23.
        switch (e.minor) {
          case 26:
          case 23:
            break;
          default:
            throw e;
        }
      }
      if (encodedLegacyCredential != null) {
        CredentialWrapper wrapper = new CredentialWrapper();
        try {
          Any anyLegacy =
            codec
              .decode_value(encodedLegacyCredential, CredentialHelper.type());
          Credential legacyCredential = CredentialHelper.extract(anyLegacy);
          // extraindo informa��es da credencial 1.5
          String loginId = legacyCredential.identifier;
          String entity = legacyCredential.owner;

          LoginInfo[] originators;
          if (!legacyCredential.delegate.equals("")) {
            originators = new LoginInfo[1];
            originators[0] =
              new LoginInfo("<unknown>", legacyCredential.delegate);
          }
          else {
            originators = new LoginInfo[0];
          }
          /*
           * campo target s� pode ser preenchido ap�s defini��o da conex�o que
           * tratar� o request. Esta defini��o � feita ap�s a valida��o da
           * cadeia
           */
          CallChain callChain =
            new CallChain("", originators, new LoginInfo(loginId, entity));
          Any anyCallChain = orb.create_any();
          CallChainHelper.insert(anyCallChain, callChain);
          byte[] encodedCallChain = codec.encode_value(anyCallChain);

          // construindo uma credencial 2.0
          CredentialData credential = new CredentialData();
          credential.bus = UNKNOWN_BUS;
          credential.login = loginId;
          credential.session = -1;
          credential.ticket = -1;
          credential.hash = LEGACY_HASH;
          credential.chain =
            new SignedCallChain(LEGACY_ENCRYPTED_BLOCK, encodedCallChain);
          // salvando informa��es no wrapper
          wrapper.isLegacy = true;
          wrapper.credential = credential;
          wrapper.legacyCredential = legacyCredential;
          return wrapper;
        }
        catch (TypeMismatch e) {
          String message =
            String.format("Falha ao decodificar a credencial 1.5: %s", e
              .getClass().getSimpleName());
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message, 0, CompletionStatus.COMPLETED_NO);
        }
        catch (FormatMismatch e) {
          String message =
            String.format("Falha ao decodificar a credencial 1.5: %s", e
              .getClass().getSimpleName());
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message, 0, CompletionStatus.COMPLETED_NO);
        }
        catch (InvalidTypeForEncoding e) {
          String message =
            String.format(
              "Falha ao construir credencial 2.0 a partir da 1.5: %s", e
                .getClass().getSimpleName());
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message, 0, CompletionStatus.COMPLETED_NO);
        }
      }
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
    int requestId = ri.request_id();
    byte[] object_id = ri.object_id();
    ORB orb = this.getMediator().getORB();
    OpenBusContextImpl context = this.getMediator().getContext();
    CredentialWrapper wrapper = retrieveCredential(ri);
    try {
      CredentialData credential = wrapper.credential;
      if (credential != null) {
        String busId = credential.bus;
        String loginId = credential.login;
        ConnectionImpl conn =
          getConnForDispatch(context, busId, loginId, object_id, operation);
        logger
          .finest(String
            .format(
              "Conex�o para o dispatch: conex�o (%s) login (%s) opera��o (%s) requestId (%s)",
              conn.connId(), conn.login().id, operation, requestId));
        context.setCurrentConnection(conn);
        boolean valid = false;
        if (!wrapper.isLegacy) {
          try {
            valid = conn.cache.logins.validateLogin(loginId);
          }
          catch (NO_PERMISSION e) {
            if (e.minor == NoLoginCode.value) {
              String message =
                "Erro ao validar o login. Conex�o dispatcher est� deslogada. opera��o (%s) requestId (%d)";
              logger.log(Level.SEVERE, String.format(message, operation,
                requestId), e);
              throw new NO_PERMISSION(UnknownBusCode.value,
                CompletionStatus.COMPLETED_NO);
            }
            else {
              String message =
                "Erro ao validar o login. opera��o (%s) requestId (%d)";
              logger.log(Level.SEVERE, String.format(message, operation,
                requestId), e);
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
          if (!valid) {
            throw new NO_PERMISSION(InvalidLoginCode.value,
              CompletionStatus.COMPLETED_NO);
          }
        }
        else {
          // caso com credencial 1.5
          try {
            if (conn.cache.logins.validateLogin(loginId)
              && conn.cache.valids.isValid(wrapper.legacyCredential)) {
              valid = true;
            }
          }
          catch (Exception e) {
            String message =
              "Erro ao validar o login 1.5  opera��o (%s) requestId (%d)";
            logger.log(Level.SEVERE, String.format(message, operation,
              requestId), e);
            throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
          }
          if (!valid) {
            String msg =
              "Login de credencial 1.5 n�o � v�lido: login (%s) opera��o (%s) requestId (%d)";
            logger.fine(String.format(msg, loginId, operation, requestId));
            throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
          }
        }
        OctetSeqHolder pubkey = new OctetSeqHolder();
        String entity;
        try {
          entity = conn.cache.logins.getLoginEntity(loginId, pubkey);
        }
        catch (InvalidLogins e) {
          String message =
            "Erro ao verificar o login. opera��o (%s) requestId (%d)";
          logger.log(Level.SEVERE,
            String.format(message, operation, requestId), e);
          throw new NO_PERMISSION(InvalidLoginCode.value,
            CompletionStatus.COMPLETED_NO);
        }
        catch (ServiceFailure e) {
          String message =
            "Erro ao verificar o login. opera��o (%s) requestId (%d)";
          logger.log(Level.SEVERE,
            String.format(message, operation, requestId), e);
          throw new NO_PERMISSION(UnverifiedLoginCode.value,
            CompletionStatus.COMPLETED_NO);
        }
        catch (NO_PERMISSION e) {
          if (e.minor == NoLoginCode.value) {
            String message =
              "Erro ao verificar o login. Conex�o dispatcher est� deslogada. opera��o (%s) requestId (%d)";
            logger.log(Level.SEVERE, String.format(message, operation,
              requestId), e);
            throw new NO_PERMISSION(UnknownBusCode.value,
              CompletionStatus.COMPLETED_NO);
          }
          else {
            String message =
              "Erro ao verificar o login. opera��o (%s) requestId (%d)";
            logger.log(Level.SEVERE, String.format(message, operation,
              requestId), e);
            throw new NO_PERMISSION(UnverifiedLoginCode.value,
              CompletionStatus.COMPLETED_NO);
          }
        }
        catch (Exception e) {
          String message = "Erro ao verificar o login.";
          logger.log(Level.SEVERE, message, e);
          throw new NO_PERMISSION(UnverifiedLoginCode.value,
            CompletionStatus.COMPLETED_NO);
        }
        if (validateCredential(credential, ri, conn)) {
          if (validateChain(credential, pubkey, ri, conn)) {
            // salvando informa��o do barramento que atendeu a requisi��o
            Any any = orb.create_any();
            any.insert_string(conn.busid());
            ri.set_slot(this.getMediator().getBusSlotId(), any);
            String msg =
              "Recebendo chamada pelo barramento: login (%s) entidade (%s) opera��o (%s) requestId (%d)";
            logger.fine(String.format(msg, loginId, entity, operation,
              requestId));
          }
          else {
            logger
              .finest(String
                .format(
                  "Recebeu chamada com cadeia inv�lida: opera��o (%s) requestId (%d)",
                  operation, requestId));
            throw new NO_PERMISSION(InvalidChainCode.value,
              CompletionStatus.COMPLETED_NO);
          }
        }
        else {
          logger
            .finest(String
              .format(
                "Recebeu chamada sem sess�o associda: opera��o (%s) requestId (%d)",
                operation, requestId));
          // credencial n�o � v�lida. Resetando a credencial da sess�o.
          doResetCredential(ri, orb, conn, loginId, pubkey.value);
          throw new NO_PERMISSION(InvalidCredentialCode.value,
            CompletionStatus.COMPLETED_NO);
        }
        // salva a conex�o utilizada no dispatch
        // CHECK se o bug relatado no finally for verdadeiro, isso pode ser desnecessario
        setCurrentConnection(ri, conn);
      }
      else {
        logger.fine(String.format(
          "Recebeu chamada fora do barramento: opera��o (%s) requestId (%d)",
          operation, requestId));
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
      // CHECK caso callback gere um erro, busco a conexao default ou NO_PERMISSION?
    }
    if (conn == null) {
      conn = (ConnectionImpl) context.getDefaultConnection();
      if (conn == null) {
        throw new NO_PERMISSION(UnknownBusCode.value,
          CompletionStatus.COMPLETED_NO);
      }
    }
    if (conn.login() == null
      || (!conn.busid().equals(busId) && !UNKNOWN_BUS.equals(busId))) {
      throw new NO_PERMISSION(UnknownBusCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    return conn;
  }

  /**
   * Realiza o protocolo de reiniciar a credencial da sess�o.
   * 
   * @param ri informa��o do request.
   * @param orb o orb.
   * @param conn a conex�o.
   * @param caller identificador do login originador da chamada.
   * @param publicKey a chave p�blica do requisitante.
   * @throws CryptographyException
   */
  private void doResetCredential(ServerRequestInfo ri, ORB orb,
    ConnectionImpl conn, String caller, byte[] publicKey)
    throws CryptographyException {
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
    Any any = orb.create_any();
    CredentialResetHelper.insert(any, reset);
    byte[] encodedCredential;
    try {
      encodedCredential = this.getMediator().getCodec().encode_value(any);
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
    logger.finest(String.format(
      "Resetando a credencial: opera��o (%s) requestId (%d)", ri.operation(),
      ri.request_id()));
    ServiceContext requestServiceContext =
      new ServiceContext(CredentialContextId.value, encodedCredential);
    ri.add_reply_service_context(requestServiceContext, false);
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
    if (Arrays.equals(credential.hash, LEGACY_HASH)) {
      // credencial OpenBus 1.5
      logger.finest("Credencial OpenBus 1.5");
      return true;
    }
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
    SignedCallChain chain = credential.chain;
    boolean isValid = false;

    if (chain != null) {
      CallChain callChain = unmarshallSignedChain(chain, logger);
      if (!Arrays.equals(chain.signature, LEGACY_ENCRYPTED_BLOCK)) {
        try {
          boolean verified =
            crypto.verifySignature(busPubKey, chain.encoded, chain.signature);
          if (verified) {
            LoginInfo loginInfo = conn.login();
            if (callChain.target.equals(loginInfo.entity)) {
              LoginInfo caller = callChain.caller;
              if (caller.id.equals(credential.login)) {
                isValid = true;
              }
            }
            else {
              ORB orb = this.getMediator().getORB();
              logger
                .finest(String
                  .format(
                    "O login n�o � o mesmo do alvo da cadeia. � necess�rio refazer a sess�o de credencial atrav�s de um reset. Opera��o: %s",
                    ri.operation()));
              // credencial n�o � v�lida. Resetando a credencial da sess�o.
              doResetCredential(ri, orb, conn, credential.login, pubkey.value);
              throw new NO_PERMISSION(InvalidCredentialCode.value,
                CompletionStatus.COMPLETED_NO);
            }
          }
        }
        catch (CryptographyException e) {
          String message =
            "Falha inesperada ao verificar assinatura da cadeia.";
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message);
        }
      }
      else {
        // cadeia 1.5 � sempre v�lida
        logger.finest("Cadeia OpenBus 1.5");
        /*
         * Atualizando a informa��o target da cadeia gerada a partir de
         * informa��es 1.5, pois foi necess�rio aguardar a defini��o da conex�o
         * que trataria a requisi�o legada
         */
        callChain.target = conn.login().entity;
        isValid = true;
      }
      if (isValid) {
        try {
          ORB orb = this.getMediator().getORB();
          // salvando a cadeia
          Any singnedAny = orb.create_any();
          SignedCallChainHelper.insert(singnedAny, chain);
          ri.set_slot(this.getMediator().getSignedChainSlotId(), singnedAny);
        }
        catch (InvalidSlot e) {
          String message =
            "Falha inesperada ao armazenar o dados no slot de contexto";
          logger.log(Level.SEVERE, message, e);
          throw new INTERNAL(message);
        }
      }
    }
    return isValid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_reply(ServerRequestInfo ri) {
    // CHECK deveria setar o currentConnection antigo?
    removeCurrentConnection(ri);

    // CHECK verificar se preciso limpar mais algum slot
    Any any = this.getMediator().getORB().create_any();
    try {
      ri.set_slot(this.getMediator().getSignedChainSlotId(), any);
      ri.set_slot(this.getMediator().getBusSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao limpar informa��es nos slots";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    String msg = "Chamada atendida: opera��o (%s) requestId (%d)";
    logger.fine(String.format(msg, ri.operation(), ri.request_id()));
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
      ORBMediator mediator = this.getMediator();
      int id = mediator.getUniqueId();
      OpenBusContextImpl context = this.getMediator().getContext();
      Any any = mediator.getORB().create_any();
      any.insert_long(id);
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
      OpenBusContextImpl context = this.getMediator().getContext();
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
      Any any = this.getMediator().getORB().create_any();
      ri.set_slot(context.getCurrentConnectionSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

  }

}
