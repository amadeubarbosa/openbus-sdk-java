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
import java.util.logging.Logger;

import org.omg.CORBA.LocalObject;

import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionMultiplexer;

final class ConnectionMultiplexerImpl extends LocalObject implements
  ConnectionMultiplexer {

  // TODO: devido a modificação semantica ente a API com ou sem multiplexação
  // queremos que lance um erro quando tenta-se realizar mais de 1 conexão no
  // modo sem multiplexação (AlreadyConnected)

  // TODO: com multiplex uma thead não pode executar uma chamada caso não
  // esteja associada a uma conexão, a não ser que só exista uma conexão.

  // TODO: no interceptador, com multiplexação, temos que guardar a conexão 
  // utilizada no receive e send request para utilizar a mesma no send e receive
  // reply

  // TODO: o setCurrentThread precisa definir no PICurrent qual a thread que foi
  // setada.

  private static final Logger logger = Logger
    .getLogger(ConnectionMultiplexerImpl.class.getName());

  /** Conexões por barramento */
  private Map<String, Set<Connection>> buses;
  /** Mapa de conexão default por barramento */
  private Map<String, Connection> busDefaultConn;
  /** Mapa de conexão por thread */
  private Map<Thread, Connection> connectedThreads;

  /**
   * Construtor.
   */
  public ConnectionMultiplexerImpl() {
    this.buses =
      Collections.synchronizedMap(new HashMap<String, Set<Connection>>());
    this.busDefaultConn =
      Collections.synchronizedMap(new HashMap<String, Connection>());
    this.connectedThreads =
      Collections.synchronizedMap(new HashMap<Thread, Connection>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection[] getConnections() {
    List<Connection> all = new ArrayList<Connection>();
    for (Entry<String, Set<Connection>> entry : buses.entrySet()) {
      all.addAll(entry.getValue());
    }
    return all.toArray(new Connection[all.size()]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCurrentConnection(Connection conn) {
    this.connectedThreads.remove(Thread.currentThread());
    if (conn != null) {
      this.connectedThreads.put(Thread.currentThread(), conn);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getCurrentConnection() {
    Connection connection = this.connectedThreads.get(Thread.currentThread());
    if (connection != null) {
      return connection;
    }
    // Caso exista uma única conexão com o barramento, esta é retornada.
    if (this.buses.size() == 1) {
      Collection<Connection> connections =
        this.buses.values().iterator().next();
      if (connections.size() == 1) {
        return connections.iterator().next();
      }
      else {
        logger
          .fine("Não foi possível obter a conexão, pois não existe conexão para o barramento");
      }
    }
    else {
      logger
        .fine("Não foi possível obter a conexão, pois não se conhece nenhum barramento");
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIncommingConnection(String busid, Connection conn) {
    this.busDefaultConn.remove(busid);
    if (conn != null) {
      this.busDefaultConn.put(busid, conn);
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
   * Método de controle para incluir uma nova conexão.
   * 
   * @param conn a conexão a ser incluida.
   */
  void addConnection(Connection conn) {
    Set<Connection> conns = this.buses.get(conn.busid());
    if (conns == null) {
      conns = Collections.synchronizedSet(new HashSet<Connection>());
      this.buses.put(conn.busid(), conns);
    }
    conns.add(conn);
  }

  /**
   * Método de controle para remover uma conexão.
   * 
   * @param conn a conexão a ser removida.
   */
  void removeConnection(Connection conn) {
    String busid = conn.busid();
    // mapa de conexões por barramentos
    if (this.buses.containsKey(busid)) {
      Set<Connection> set = this.buses.get(busid);
      set.remove(conn);
    }
    // mapa de conexão default por barramento
    if (this.busDefaultConn.containsKey(busid)) {
      Connection defconn = this.busDefaultConn.get(busid);
      if (defconn.equals(conn)) {
        this.busDefaultConn.remove(busid);
      }
    }
    // mapa de conexão por thread
    List<Thread> toRemove = new ArrayList<Thread>();
    for (Entry<Thread, Connection> entry : this.connectedThreads.entrySet()) {
      if (entry.getValue().equals(conn)) {
        toRemove.add(entry.getKey());
      }
    }
    for (Thread thread : toRemove) {
      this.connectedThreads.remove(thread);
    }
  }

  boolean hasBus(String busid) {
    return this.buses.containsKey(busid);
  }
}
