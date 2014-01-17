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
 * Implementa��o do multiplexador de conex�o.
 * 
 * @author Tecgraf
 */
final class OpenBusContextImpl extends LocalObject implements OpenBusContext {

  /** Logger. */
  private static final Logger logger = Logger
    .getLogger(OpenBusContextImpl.class.getName());

  /** Identificador do slot de conexao corrente */
  private final int CURRENT_CONNECTION_SLOT_ID;
  /** Identificador do slot de intercepta��o ignorada */
  private final int IGNORE_THREAD_SLOT_ID;
  /** Mapa de conex�o por Requester */
  private Map<Integer, Connection> connectedById;
  /** Conex�o padr�o */
  private Connection defaultConn;
  /** Callback a ser disparada caso o login se encontre inv�lido */
  private CallDispatchCallback dispatchCallback;

  /** Refer�ncia para o ORB ao qual pertence */
  private ORB orb;

  /** Lock para opera��es sobre conex�es */
  private final ReentrantReadWriteLock rwlock =
    new ReentrantReadWriteLock(true);
  /** Lock de leitura para opera��es sobre conex�es */
  private final ReadLock readLock = rwlock.readLock();
  /** Lock de escrita para opera��es sobre conex�es */
  private final WriteLock writeLock = rwlock.writeLock();

  /**
   * Construtor.
   * 
   * @param currentConnectionSlotId identificador do slot da conex�o corrente
   * @param ignoreThreadSlotId identificador do slot de intercepta��o ignorada.
   */
  public OpenBusContextImpl(int currentConnectionSlotId, int ignoreThreadSlotId) {
    this.connectedById =
      Collections.synchronizedMap(new HashMap<Integer, Connection>());
    this.CURRENT_CONNECTION_SLOT_ID = currentConnectionSlotId;
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
   * Recupera a chave do slot de identifica��o da conex�o corrente.
   * 
   * @return a chave do slot.s
   */
  int getCurrentConnectionSlotId() {
    return this.CURRENT_CONNECTION_SLOT_ID;
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
    int id = ORBUtils.getMediator(orb).getUniqueId();
    Any any = this.orb.create_any();
    if (conn != null) {
      any.insert_long(id);
    }
    Current current = ORBUtils.getPICurrent(orb);
    try {
      current.set_slot(CURRENT_CONNECTION_SLOT_ID, any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return setConnectionById(id, conn);
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
      any = current.get_slot(CURRENT_CONNECTION_SLOT_ID);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao acessar o slot da thread corrente";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }

    if (any.type().kind().value() != TCKind._tk_null) {
      int id = any.extract_long();
      connection = this.connectedById.get(id);
    }

    if (connection == null) {
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
      Any targetAny = this.orb.create_any();
      targetAny.insert_string(chain.target());
      current.set_slot(mediator.getJoinedChainTargetSlotId(), targetAny);
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
      current.set_slot(mediator.getJoinedChainTargetSlotId(), any);
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
      any = current.get_slot(mediator.getJoinedChainTargetSlotId());
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      String target = any.extract_string();
      return new CallerChainImpl(busId, target, callChain.caller,
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
   * Recupera a conex�o associada ao identificador.
   * 
   * @param id o identificador
   * @return a conex�o em uso.
   */
  Connection getConnectionById(int id) {
    return this.connectedById.get(id);
  }

  /**
   * Configura a conex�o em uso para o identificador especificado..
   * 
   * @param id identificador.
   * @param conn a conex�o em uso.
   * @return a antiga conex�o configurada.
   */
  Connection setConnectionById(int id, Connection conn) {
    synchronized (this.connectedById) {
      Connection old = this.connectedById.remove(id);
      if (conn != null) {
        this.connectedById.put(id, conn);
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
   * Sinaliza que as requisi��es realizadas atrav�s desta thread n�o devem ser
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
        "Falha inesperada ao acessar o slot de intercepta��o ignorada";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Siznaliza que as requisi��es realizadas atrav�s desta thread devem voltar a
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
        "Falha inesperada ao acessar o slot de intercepta��o ignorada";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
  }

  /**
   * Verifica se a requisi��o corrente esta configurada para ignorar a
   * intercepta��o ou n�o.
   * 
   * @param ri informa��o da requisi��o.
   * @return <code>true</code> caso a intercepta��o deve ser ignorada, e
   *         <code>false</code> caso contr�rio.
   */
  boolean isCurrentThreadIgnored(RequestInfo ri) {
    Any any;
    try {
      any = ri.get_slot(IGNORE_THREAD_SLOT_ID);
    }
    catch (InvalidSlot e) {
      String message =
        "Falha inesperada ao obter o slot de intercepta��o ignorada";
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
