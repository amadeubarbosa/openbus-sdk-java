package tecgraf.openbus.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.core.v2_00.BusObjectKey;
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
  /** Mapa de conex�o que trata requisi��es de entrada por barramento */
  private Map<String, Connection> incomingDispatcherConn;
  /** Mapa de conex�o por thread */
  private Map<Long, Connection> connectedThreads;
  /** Conex�o padr�o */
  private Connection defaultConn;
  /** Threads com intercepta��o ignorada */
  private Set<Thread> ignoredThreads;

  /** Refer�ncia para o ORB ao qual pertence */
  private ORB orb;

  /**
   * Construtor.
   * 
   * @param currentThreadSlotId identificador do slot da thread corrente
   */
  public ConnectionManagerImpl(int currentThreadSlotId) {
    this.incomingDispatcherConn =
      Collections.synchronizedMap(new HashMap<String, Connection>());
    this.connectedThreads =
      Collections.synchronizedMap(new HashMap<Long, Connection>());
    this.ignoredThreads = Collections.synchronizedSet(new HashSet<Thread>());
    this.CURRENT_THREAD_SLOT_ID = currentThreadSlotId;
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
    ignoreCurrentThread();
    try {
      String str =
        String.format("corbaloc::1.0@%s:%d/%s", host, port, BusObjectKey.value);
      org.omg.CORBA.Object obj = orb.string_to_object(str);
      if (obj == null) {
        return null;
      }
      IComponent component = IComponentHelper.narrow(obj);
      BusInfo bus = new BusInfo(component);
      ConnectionImpl conn = new ConnectionImpl(bus, orb);
      /*
       * enquanto n�o definimos uma API o legacy esta guardado no ORBMediator
       * por�m, esta sempre true. Este � o ponto de entrada para configurar o
       * legacy se for por conex�o. Caso seja por ORB, colocar no ORBInit?
       */
      boolean legacy = true;
      if (legacy) {
        String legacyStr =
          String.format("corbaloc::1.0@%s:%d/%s", host, port, "openbus_v1_05");
        org.omg.CORBA.Object legacyObj = orb.string_to_object(legacyStr);
        if (legacyObj != null) {
          IComponent legacyComponent = IComponentHelper.narrow(legacyObj);
          LegacyInfo legacyBus = new LegacyInfo(legacyComponent);
          conn.setLegacyInfo(legacyBus);
        }
      }
      return conn;
    }
    finally {
      unignoreCurrentThread();
    }
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
  public void setThreadRequester(Connection conn) {
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
  public Connection getThreadRequester() {
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
  public void setupBusDispatcher(Connection conn) {
    synchronized (this.incomingDispatcherConn) {
      this.incomingDispatcherConn.remove(conn.busid());
      if (conn != null) {
        this.incomingDispatcherConn.put(conn.busid(), conn);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getBusDispatcher(String busid) {
    return this.incomingDispatcherConn.get(busid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection removeBusDispatcher(String busid) {
    return this.incomingDispatcherConn.remove(busid);
  }

  /**
   * Recupera a lista de conex�es de despacho.
   * 
   * @return A lista de conex�es de despacho.
   */
  Collection<Connection> getIncommingConnections() {
    return this.incomingDispatcherConn.values();
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

  void ignoreCurrentThread() {
    Thread currentThread = Thread.currentThread();
    this.ignoredThreads.add(currentThread);
  }

  void unignoreCurrentThread() {
    Thread currentThread = Thread.currentThread();
    this.ignoredThreads.remove(currentThread);
  }

  boolean isCurrentThreadIgnored() {
    Thread currentThread = Thread.currentThread();
    return this.ignoredThreads.contains(currentThread);
  }

}
