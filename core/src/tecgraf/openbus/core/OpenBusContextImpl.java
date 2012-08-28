package tecgraf.openbus.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UserException;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.credential.SignedCallChainHelper;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.InvalidPropertyValue;
import tecgraf.openbus.exception.OpenBusInternalException;

/**
 * Implementação do multiplexador de conexão.
 * 
 * @author Tecgraf
 */
final class OpenBusContextImpl extends LocalObject implements OpenBusContext {

  /** Logger. */
  private static final Logger logger = Logger
    .getLogger(OpenBusContextImpl.class.getName());

  /** Identificador do slot de thread corrente */
  private final int CURRENT_THREAD_SLOT_ID;
  /** Identificador do slot de interceptação ignorada */
  private final int IGNORE_THREAD_SLOT_ID;
  /** Mapa de conexão por Requester */
  private Map<Long, Connection> connectedThreads;
  /** Mapa de thread para cadeia de chamada */
  private Map<Thread, CallerChain> joinedChains;
  /** Conexão padrão */
  private Connection defaultConn;
  /** Callback a ser disparada caso o login se encontre inválido */
  private CallDispatchCallback dispatchCallback;

  /** Referência para o ORB ao qual pertence */
  private ORB orb;

  /** Lock para operações sobre conexões */
  private final ReentrantReadWriteLock rwlock =
    new ReentrantReadWriteLock(true);
  /** Lock de leitura para operações sobre conexões */
  private final ReadLock readLock = rwlock.readLock();
  /** Lock de escrita para operações sobre conexões */
  private final WriteLock writeLock = rwlock.writeLock();

  /**
   * Construtor.
   * 
   * @param currentThreadSlotId identificador do slot da thread corrente
   * @param ignoreThreadSlotId identificador do slot de interceptação ignorada.
   */
  public OpenBusContextImpl(int currentThreadSlotId, int ignoreThreadSlotId) {
    this.connectedThreads =
      Collections.synchronizedMap(new HashMap<Long, Connection>());
    this.joinedChains =
      Collections.synchronizedMap(new HashMap<Thread, CallerChain>());
    this.CURRENT_THREAD_SLOT_ID = currentThreadSlotId;
    this.IGNORE_THREAD_SLOT_ID = ignoreThreadSlotId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ORB orb() {
    return this.orb;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection createConnection(String host, int port) {
    ConnectionImpl conn;
    try {
      conn = new ConnectionImpl(host, port, this, orb);
    }
    catch (InvalidPropertyValue e) {
      // Nunca deveria acontecer
      throw new OpenBusInternalException(
        "BUG: Este erro nunca deveria ocorrer.", e);
    }
    return conn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection createConnection(String host, int port, Properties props)
    throws InvalidPropertyValue {
    return new ConnectionImpl(host, port, this, orb, props);
  }

  /**
   * Recupera a chave do slot de identificação da thread corrente.
   * 
   * @return a chave do slot.s
   */
  int getCurrentThreadSlotId() {
    return this.CURRENT_THREAD_SLOT_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection setDefaultConnection(Connection conn) {
    Connection old;
    this.writeLock.lock();
    try {
      old = this.defaultConn;
      this.defaultConn = conn;
    }
    finally {
      this.writeLock.unlock();
    }
    return old;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getDefaultConnection() {
    this.readLock.lock();
    try {
      return this.defaultConn;
    }
    finally {
      this.readLock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection setCurrentConnection(Connection conn) {
    long id = Thread.currentThread().getId();
    Any any = this.orb.create_any();
    any.insert_longlong(id);
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(CURRENT_THREAD_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return setConnectionByThreadId(id, conn);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getCurrentConnection() {
    Connection connection = null;
    Current current = ORBUtils.getPICurrent(orb);
    Any any;
    try {
      any = current.get_slot(CURRENT_THREAD_SLOT_ID);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

    if (any.type().kind().value() != TCKind._tk_null) {
      long id = any.extract_longlong();
      connection = this.connectedThreads.get(id);
    }
    else {
      connection = getDefaultConnection();
    }
    return connection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain getCallerChain() {
    Current current = ORBUtils.getPICurrent(orb);
    ORBMediator mediator = ORBUtils.getMediator(orb);
    String busId;
    CallChain callChain;
    SignedCallChain signedChain;
    try {
      Any any = current.get_slot(mediator.getBusSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      busId = any.extract_string();
      any = current.get_slot(mediator.getSignedChainSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      signedChain = SignedCallChainHelper.extract(any);
      Any anyChain =
        mediator.getCodec().decode_value(signedChain.encoded,
          CallChainHelper.type());
      callChain = CallChainHelper.extract(anyChain);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao recuperar a cadeia.";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return new CallerChainImpl(busId, callChain.target, callChain.caller,
      callChain.originators, signedChain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joinChain() throws OpenBusInternalException {
    joinChain(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void joinChain(CallerChain chain) {
    chain = (chain != null) ? chain : getCallerChain();
    if (chain == null) {
      return;
    }
    try {
      Current current = ORBUtils.getPICurrent(orb);
      ORBMediator mediator = ORBUtils.getMediator(orb);
      SignedCallChain signedChain = ((CallerChainImpl) chain).signedCallChain();
      Any any = this.orb.create_any();
      SignedCallChainHelper.insert(any, signedChain);
      current.set_slot(mediator.getJoinedChainSlotId(), any);
      Any busAny = this.orb.create_any();
      busAny.insert_string(chain.busid());
      current.set_slot(mediator.getJoinedBusSlotId(), busAny);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void exitChain() {
    try {
      Current current = ORBUtils.getPICurrent(orb);
      ORBMediator mediator = ORBUtils.getMediator(orb);
      Any any = this.orb.create_any();
      current.set_slot(mediator.getJoinedChainSlotId(), any);
      current.set_slot(mediator.getJoinedBusSlotId(), any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallerChain getJoinedChain() {
    try {
      Current current = ORBUtils.getPICurrent(orb);
      ORBMediator mediator = ORBUtils.getMediator(orb);

      Any any = current.get_slot(mediator.getJoinedBusSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      String busId = any.extract_string();

      any = current.get_slot(mediator.getJoinedChainSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      SignedCallChain signedChain = SignedCallChainHelper.extract(any);
      Any anyChain =
        mediator.getCodec().decode_value(signedChain.encoded,
          CallChainHelper.type());
      CallChain callChain = CallChainHelper.extract(anyChain);
      return new CallerChainImpl(busId, callChain.target, callChain.caller,
        callChain.originators, signedChain);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao recuperar a cadeia.";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Recupera a conexão dado a identificação da thread.
   * 
   * @param threadId a identificação da thread
   * @return a conexão em uso.
   */
  Connection getConnectionByThreadId(long threadId) {
    return this.connectedThreads.get(threadId);
  }

  /**
   * Configura a conexão em uso na thread.
   * 
   * @param threadId identificador da thread.
   * @param conn a conexão em uso.
   * @return a antiga conexão configurada.
   */
  Connection setConnectionByThreadId(long threadId, Connection conn) {
    synchronized (this.connectedThreads) {
      Connection old = this.connectedThreads.remove(threadId);
      if (conn != null) {
        this.connectedThreads.put(threadId, conn);
      }
      return old;
    }
  }

  /**
   * Configura o ORB que o multiplexador esta associado.
   * 
   * @param orb o ORB.
   */
  void setORB(ORB orb) {
    this.orb = orb;
  }

  /**
   * Sinaliza que as requisições realizadas através desta thread não devem ser
   * interceptadas.
   */
  void ignoreCurrentThread() {
    Any any = this.orb.create_any();
    any.insert_boolean(true);
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(IGNORE_THREAD_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message =
        "Falha inesperada ao acessar o slot de interceptação ignorada";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Siznaliza que as requisições realizadas através desta thread devem voltar a
   * ser interceptadas.
   */
  void unignoreCurrentThread() {
    Any any = this.orb.create_any();
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(IGNORE_THREAD_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message =
        "Falha inesperada ao acessar o slot de interceptação ignorada";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Verifica se a requisição corrente esta configurada para ignorar a
   * interceptação ou não.
   * 
   * @param ri informação da requisição.
   * @return <code>true</code> caso a interceptação deve ser ignorada, e
   *         <code>false</code> caso contrário.
   */
  boolean isCurrentThreadIgnored(RequestInfo ri) {
    Any any;
    try {
      any = ri.get_slot(IGNORE_THREAD_SLOT_ID);
    }
    catch (InvalidSlot e) {
      String message =
        "Falha inesperada ao obter o slot de interceptação ignorada";
      throw new INTERNAL(message);
    }
    if (any.type().kind().value() != TCKind._tk_null) {
      boolean isIgnored = any.extract_boolean();
      return isIgnored;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCallDispatch(CallDispatchCallback callback) {
    this.dispatchCallback = callback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CallDispatchCallback onCallDispatch() {
    return this.dispatchCallback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginRegistry getLoginRegistry() {
    ConnectionImpl conn = (ConnectionImpl) getCurrentConnection();
    if (conn == null || conn.login() == null) {
      throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus.COMPLETED_NO);
    }
    return conn.logins();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OfferRegistry getOfferRegistry() {
    ConnectionImpl conn = (ConnectionImpl) getCurrentConnection();
    if (conn == null || conn.login() == null) {
      throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus.COMPLETED_NO);
    }
    return conn.offers();
  }

}
