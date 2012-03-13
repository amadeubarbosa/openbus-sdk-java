package tecgraf.openbus.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.Bus;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.ConnectionObserver;
import tecgraf.openbus.core.v2_00.BusObjectKey;
import tecgraf.openbus.core.v2_00.credential.CredentialData;
import tecgraf.openbus.core.v2_00.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_00.services.access_control.CallChain;
import tecgraf.openbus.core.v2_00.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.InternalException;

public final class BusORBImpl implements BusORB, ConnectionObserver {
  private static final Logger logger = Logger.getLogger(BusORBImpl.class
    .getName());

  private ORB orb;
  private ORBMediator mediator;
  private Map<String, Bus> buses;
  private Set<Thread> ignoredThreads;
  private Map<Thread, Connection> connectedThreads;

  public BusORBImpl() throws InternalException {
    this(null, null);
  }

  public BusORBImpl(String[] args) throws InternalException {
    this(args, null);
  }

  public BusORBImpl(String[] args, Properties props) throws InternalException {
    this.orb = createORB(args, props);
    this.mediator = getMediator(this.orb);
    this.mediator.setORB(this);
    this.buses = new HashMap<String, Bus>();
    this.ignoredThreads = new HashSet<Thread>();
    this.connectedThreads = new WeakHashMap<Thread, Connection>();
  }

  private static ORB createORB(String[] args, Properties props) {
    ORBBuilder orbBuilder = new ORBBuilder(args, props);
    orbBuilder.addInitializer(new ORBInitializerInfo(ORBInitializerImpl.class));
    return orbBuilder.build();
  }

  private static ORBMediator getMediator(ORB orb) throws InternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = orb.resolve_initial_references(ORBMediator.INITIAL_REFERENCE_ID);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o mediador";
      logger.log(Level.SEVERE, message, e);
      throw new InternalException(message, e);
    }
    return (ORBMediator) obj;
  }

  @Override
  public Bus getBus(String host, int port) throws CryptographyException {
    this.ignoreCurrentThread();
    try {
      org.omg.CORBA.Object obj =
        this.fetchObject(host, port, BusObjectKey.value);
      if (obj == null) {
        return null;
      }
      IComponent component = IComponentHelper.narrow(obj);
      Bus bus = new BusImpl(this, component);
      this.buses.put(bus.getId(), bus);
      return bus;
    }
    finally {
      this.unignoreCurrentThread();
    }
  }

  @Override
  public Bus hasBus(String busid) {
    return this.buses.get(busid);
  }

  public org.omg.CORBA.Object fetchObject(String host, int port, String key) {
    String str = String.format("corbaloc::1.0@%s:%d/%s", host, port, key);
    return this.orb.string_to_object(str);
  }

  public Bus getBus(String id) {
    return this.buses.get(id);
  }

  @Override
  public Connection getCurrentConnection() {
    Connection connection = this.connectedThreads.get(Thread.currentThread());
    if (connection != null) {
      return connection;
    }
    logger.fine("Sem conexão definida na thread corrente");

    // Caso exista uma única conexão com o barramento, esta é retornada.
    if (this.buses.size() == 1) {
      Bus bus = this.buses.values().iterator().next();
      Collection<Connection> connections = bus.getConnections();
      if (connections.size() == 1) {
        return connections.iterator().next();
      }
      else {
        logger
          .fine("Não foi possível obter a conexão, pois não há uma única conexão");
      }
    }
    else {
      logger
        .fine("Não foi possível obter a conexão, pois não há um único barramento");
    }

    return null;
  }

  @Override
  public void setCurrentConnection(Connection connection) {
    Connection currentConnection =
      this.connectedThreads.remove(Thread.currentThread());
    if (currentConnection != null) {
      currentConnection.removeObserver(this);
    }
    if (connection != null) {
      connection.addObserver(this);
      this.connectedThreads.put(Thread.currentThread(), connection);
    }
  }

  @Override
  public CallerChain getCallerChain() throws InternalException {
    Current current = getPICurrent(this.orb);
    String busId;
    CallChain callChain;
    try {
      Any any = current.get_slot(this.mediator.getCredentialSlotId());
      CredentialData credential = CredentialDataHelper.extract(any);
      busId = credential.bus;
      Any anyChain =
        this.getCodec().decode_value(credential.chain.encoded,
          CallChainHelper.type());
      callChain = CallChainHelper.extract(anyChain);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new InternalException(message, e);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao decodificar a cadeia de chamadas.";
      logger.log(Level.SEVERE, message, e);
      throw new InternalException(message, e);
    }
    LoginInfo[] callers = callChain.callers;
    Bus bus = this.buses.get(busId);
    if (bus == null) {
      String message =
        String.format(
          "Uma credencial de um barramento desconhecido (%s) foi recebida",
          busId);
      logger.log(Level.SEVERE, message);
      return null;
    }

    return new CallerChainImpl(bus, callers);
  }

  private static Current getPICurrent(ORB orb) throws InternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = orb.resolve_initial_references("PICurrent");
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new InternalException(message, e);
    }
    return CurrentHelper.narrow(obj);
  }

  CallerChain getCallerChain(String id) throws InternalException {
    CallerChain chain = this.getCallerChain();
    if (chain == null) {
      return null;
    }

    if (chain.getBus().getId().equals(id)) {
      return chain;
    }
    return null;
  }

  @Override
  public void ignoreCurrentThread() {
    Thread currentThread = Thread.currentThread();
    this.ignoredThreads.add(currentThread);
  }

  @Override
  public void unignoreCurrentThread() {
    Thread currentThread = Thread.currentThread();
    this.ignoredThreads.remove(currentThread);
  }

  @Override
  public boolean isCurrentThreadIgnored() {
    Thread currentThread = Thread.currentThread();
    return this.ignoredThreads.contains(currentThread);
  }

  @Override
  public ORB getORB() {
    return this.orb;
  }

  @Override
  public POA getRootPOA() throws InternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = this.orb.resolve_initial_references("RootPOA");
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o RootPOA";
      logger.log(Level.SEVERE, message, e);
      throw new InternalException(message, e);
    }
    return POAHelper.narrow(obj);
  }

  @Override
  public Codec getCodec() {
    return this.mediator.getCodec();
  }

  @Override
  public void connectionClosed(Connection connection) {
    for (Thread thread : this.connectedThreads.keySet()) {
      Connection mappedConnection = this.connectedThreads.get(thread);
      if (connection.equals(mappedConnection)) {
        this.connectedThreads.remove(thread);
      }
    }
  }
}
