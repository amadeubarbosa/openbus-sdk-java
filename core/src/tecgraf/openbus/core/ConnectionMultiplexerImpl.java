package tecgraf.openbus.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.TCKind;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionMultiplexer;
import tecgraf.openbus.core.v2_00.services.access_control.NoLoginCode;
import tecgraf.openbus.exception.OpenBusInternalException;

/**
 * Implementa��o do multiplexador de conex�o.
 * 
 * @author Tecgraf
 */
final class ConnectionMultiplexerImpl extends LocalObject implements
  ConnectionMultiplexer {

  /** Logger. */
  private static final Logger logger = Logger
    .getLogger(ConnectionMultiplexerImpl.class.getName());

  /** Identificador do slot de thread corrente */
  private final int CURRENT_THREAD_SLOT_ID;
  /** Conex�es por barramento */
  private Map<String, Set<Connection>> buses;
  /** Mapa de conex�o default por barramento */
  private Map<String, Connection> busDefaultConn;
  /** Mapa de conex�o por thread */
  private Map<Long, Connection> connectedThreads;

  /** Flag que indica se a api utilizada � com multiplexa��o ou n�o. */
  private boolean isMultiplexed = false;
  /** Refer�ncia para o ORB ao qual pertence */
  private BusORBImpl orb;

  /**
   * Construtor.
   * 
   * @param currentThreadSlotId identificador do slot da thread corrente
   */
  public ConnectionMultiplexerImpl(int currentThreadSlotId) {
    this.buses =
      Collections.synchronizedMap(new HashMap<String, Set<Connection>>());
    this.busDefaultConn =
      Collections.synchronizedMap(new HashMap<String, Connection>());
    this.connectedThreads =
      Collections.synchronizedMap(new HashMap<Long, Connection>());
    this.CURRENT_THREAD_SLOT_ID = currentThreadSlotId;
  }

  int getCurrentThreadSlotId() {
    return this.CURRENT_THREAD_SLOT_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection[] getConnections() {
    List<Connection> all = new ArrayList<Connection>();
    synchronized (buses) {
      for (Entry<String, Set<Connection>> entry : buses.entrySet()) {
        all.addAll(entry.getValue());
      }
    }
    return all.toArray(new Connection[all.size()]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCurrentConnection(Connection conn) {
    long id = Thread.currentThread().getId();
    Any any = this.orb.getORB().create_any();
    any.insert_longlong(id);
    Current current = this.orb.getPICurrent();
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
  public Connection getCurrentConnection() {
    if (isMultiplexed()) {
      Current current = this.orb.getPICurrent();
      Any any;
      try {
        any = current.get_slot(CURRENT_THREAD_SLOT_ID);
      }
      catch (InvalidSlot e) {
        String message =
          "Falha inesperada ao acessar o slot da thread corrente";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }

      if (any.type().kind().value() != TCKind._tk_null) {
        long id = any.extract_longlong();
        Connection connection = this.connectedThreads.get(id);
        if (connection != null) {
          return connection;
        }
      }
    }

    // Caso exista uma �nica conex�o com o barramento, esta � retornada.
    Connection connection = hasOnlyOneConnection();
    if (connection == null) {
      throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus.COMPLETED_NO);
    }
    else {
      return connection;
    }
  }

  /**
   * Recupear a conex�o, caso s� exista uma.
   * 
   * @return a conex�o.
   */
  Connection hasOnlyOneConnection() {
    synchronized (buses) {
      if (this.buses.size() == 1) {
        Collection<Connection> connections =
          this.buses.values().iterator().next();
        if (connections.size() == 1) {
          return connections.iterator().next();
        }
        else {
          logger
            .fine("N�o foi poss�vel obter a conex�o, pois n�o existe conex�o para o barramento");
        }
      }
      else if (this.buses.isEmpty()) {
        logger
          .fine("N�o foi poss�vel obter a conex�o, pois n�o se conhece nenhum barramento");
      }
      else {
        logger
          .fine("N�o foi poss�vel obter a conex�o, pois existe conex�o para mais de um barramento.");
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIncommingConnection(String busid, Connection conn) {
    synchronized (this.busDefaultConn) {
      this.busDefaultConn.remove(busid);
      if (conn != null) {
        this.busDefaultConn.put(busid, conn);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getIncommingConnection(String busid) {
    return this.busDefaultConn.get(busid);
  }

  /**
   * M�todo de controle para incluir uma nova conex�o.
   * 
   * @param conn a conex�o a ser incluida.
   */
  void addConnection(Connection conn) {
    synchronized (buses) {
      Set<Connection> conns = this.buses.get(conn.busid());
      if (conns == null) {
        conns = Collections.synchronizedSet(new HashSet<Connection>());
        this.buses.put(conn.busid(), conns);
      }
      conns.add(conn);
    }
  }

  Connection getConnectionByThreadId(long threadId) {
    return this.connectedThreads.get(threadId);
  }

  void setConnectionByThreadId(long threadId, Connection conn) {
    synchronized (this.connectedThreads) {
      this.connectedThreads.remove(threadId);
      if (conn != null) {
        this.connectedThreads.put(threadId, conn);
      }
    }
  }

  /**
   * M�todo de controle para remover uma conex�o.
   * 
   * @param conn a conex�o a ser removida.
   */
  void removeConnection(Connection conn) {
    String busid = conn.busid();
    // mapa de conex�es por barramentos
    synchronized (buses) {
      Set<Connection> set = this.buses.get(busid);
      if (set != null) {
        set.remove(conn);
      }
    }
    // mapa de conex�o default por barramento
    synchronized (this.busDefaultConn) {
      Connection defconn = this.busDefaultConn.get(busid);
      if (defconn != null) {
        if (defconn.equals(conn)) {
          this.busDefaultConn.remove(busid);
        }
      }
    }
    // mapa de conex�o por thread
    List<Long> toRemove = new ArrayList<Long>();
    synchronized (this.connectedThreads) {
      for (Entry<Long, Connection> entry : this.connectedThreads.entrySet()) {
        if (entry.getValue().equals(conn)) {
          toRemove.add(entry.getKey());
        }
      }
      for (Long id : toRemove) {
        this.connectedThreads.remove(id);
      }
    }
  }

  boolean hasBus(String busid) {
    return this.buses.containsKey(busid);
  }

  // CHECK corrigir visibilidade ou mover especializa��es de OpenBus
  void isMultiplexed(boolean isMultiplexed) {
    this.isMultiplexed = isMultiplexed;
  }

  // CHECK corrigir visibilidade ou mover especializa��es de OpenBus
  boolean isMultiplexed() {
    return this.isMultiplexed;
  }

  void setORB(BusORBImpl orb) {
    this.orb = orb;
  }

}
