package tecgraf.openbus.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.RequestInfo;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.exception.InvalidBusAddress;
import tecgraf.openbus.exception.OpenBusInternalException;

/**
 * Implementa��o do multiplexador de conex�o.
 * 
 * @author Tecgraf
 */
final class ConnectionManagerImpl extends LocalObject implements
  ConnectionManager {

  /** Logger. */
  private static final Logger logger = Logger
    .getLogger(ConnectionManagerImpl.class.getName());

  /** Identificador do slot de thread corrente */
  private final int CURRENT_THREAD_SLOT_ID;
  /** Identificador do slot de intercepta��o ignorada */
  private final int IGNORE_THREAD_SLOT_ID;
  /** Mapa de conex�o que trata requisi��es de entrada por barramento */
  private Map<String, Connection> incomingDispatcherConn;
  /** Mapa de conex�o por Requester */
  private Map<Long, Connection> connectedThreads;
  /** Conex�o padr�o */
  private Connection defaultConn;

  /** Refer�ncia para o ORB ao qual pertence */
  private ORB orb;

  /**
   * Construtor.
   * 
   * @param currentThreadSlotId identificador do slot da thread corrente
   * @param ignoreThreadSlotId identificador do slot de intercepta��o ignorada.
   */
  public ConnectionManagerImpl(int currentThreadSlotId, int ignoreThreadSlotId) {
    this.incomingDispatcherConn =
      Collections.synchronizedMap(new HashMap<String, Connection>());
    this.connectedThreads =
      Collections.synchronizedMap(new HashMap<Long, Connection>());
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
  public Connection createConnection(String host, int port)
    throws InvalidBusAddress {
    return new ConnectionImpl(host, port, this, orb);
  }

  /**
   * Recupera a chave do slot de identifica��o da thread corrente.
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
  public void setDefaultConnection(Connection conn) {
    this.defaultConn = conn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getDefaultConnection() {
    return this.defaultConn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setRequester(Connection conn) {
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
    setConnectionByThreadId(id, conn);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getRequester() {
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
    return connection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDispatcher(Connection conn) {
    if (conn == null) {
      throw new NullPointerException("Conex�o n�o pode ser nula");
    }
    synchronized (this.incomingDispatcherConn) {
      this.incomingDispatcherConn.put(conn.busid(), conn);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getDispatcher(String busid) {
    return this.incomingDispatcherConn.get(busid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection clearDispatcher(String busid) {
    return this.incomingDispatcherConn.remove(busid);
  }

  /**
   * Recupera a lista de conex�es de despacho.
   * 
   * @return A lista de conex�es de despacho.
   */
  Collection<Connection> getIncommingConnections() {
    List<Connection> list =
      new ArrayList<Connection>(this.incomingDispatcherConn.values());
    list.add(defaultConn);
    return list;
  }

  /**
   * Recupera a conex�o dado a identifica��o da thread.
   * 
   * @param threadId a identifica��o da thread
   * @return a conex�o em uso.
   */
  Connection getConnectionByThreadId(long threadId) {
    return this.connectedThreads.get(threadId);
  }

  /**
   * Configura a conex�o em uso na thread.
   * 
   * @param threadId identificador da thread.
   * @param conn a conex�o em uso.
   */
  void setConnectionByThreadId(long threadId, Connection conn) {
    synchronized (this.connectedThreads) {
      this.connectedThreads.remove(threadId);
      if (conn != null) {
        this.connectedThreads.put(threadId, conn);
      }
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

}
