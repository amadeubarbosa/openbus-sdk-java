package tecgraf.openbus.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.ConnectionMultiplexer;
import tecgraf.openbus.core.v2_00.credential.CredentialData;
import tecgraf.openbus.core.v2_00.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_00.services.access_control.CallChain;
import tecgraf.openbus.core.v2_00.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChain;
import tecgraf.openbus.exception.OpenBusInternalException;

import com.sun.jdi.InternalException;

public final class BusORBImpl implements BusORB {
  private static final Logger logger = Logger.getLogger(BusORBImpl.class
    .getName());

  private ORB orb;
  private ORBMediator mediator;
  private Set<Thread> ignoredThreads;

  public BusORBImpl() throws OpenBusInternalException {
    this(null, null);
  }

  public BusORBImpl(String[] args) throws OpenBusInternalException {
    this(args, null);
  }

  public BusORBImpl(String[] args, Properties props)
    throws OpenBusInternalException {
    this.orb = createORB(args, props);
    this.mediator = getMediator(this.orb);
    this.mediator.setORB(this);
    this.ignoredThreads = Collections.synchronizedSet(new HashSet<Thread>());
  }

  private ORB createORB(String[] args, Properties props) {
    ORBBuilder orbBuilder = new ORBBuilder(args, props);
    orbBuilder.addInitializer(new ORBInitializerInfo(ORBInitializerImpl.class));
    return orbBuilder.build();
  }

  private ORBMediator getMediator(ORB orb) throws OpenBusInternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = orb.resolve_initial_references(ORBMediator.INITIAL_REFERENCE_ID);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o mediador";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return (ORBMediator) obj;
  }

  ConnectionMultiplexerImpl getConnectionMultiplexer() {
    org.omg.CORBA.Object obj;
    try {
      obj =
        orb
          .resolve_initial_references(ConnectionMultiplexer.INITIAL_REFERENCE_ID);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o multiplexador";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return (ConnectionMultiplexerImpl) obj;
  }

  @Override
  public CallerChain getCallerChain() throws InternalException {
    Current current = getPICurrent(this.orb);
    String busId;
    CallChain callChain;
    SignedCallChain signedChain;
    try {
      Any any = current.get_slot(this.mediator.getCredentialSlotId());
      CredentialData credential = CredentialDataHelper.extract(any);
      busId = credential.bus;
      signedChain = credential.chain;
      Any anyChain =
        this.getCodec().decode_value(signedChain.encoded,
          CallChainHelper.type());
      callChain = CallChainHelper.extract(anyChain);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao obter o slot no PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    catch (UserException e) {
      String message = "Falha inesperada ao decodificar a cadeia de chamadas.";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    LoginInfo[] callers = callChain.callers;
    return new CallerChainImpl(busId, callers, signedChain);
  }

  private static Current getPICurrent(ORB orb) throws OpenBusInternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = orb.resolve_initial_references("PICurrent");
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o PICurrent";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return CurrentHelper.narrow(obj);
  }

  public void ignoreCurrentThread() {
    Thread currentThread = Thread.currentThread();
    this.ignoredThreads.add(currentThread);
  }

  public void unignoreCurrentThread() {
    Thread currentThread = Thread.currentThread();
    this.ignoredThreads.remove(currentThread);
  }

  public boolean isCurrentThreadIgnored() {
    Thread currentThread = Thread.currentThread();
    return this.ignoredThreads.contains(currentThread);
  }

  @Override
  public ORB getORB() {
    return this.orb;
  }

  @Override
  public POA getRootPOA() throws OpenBusInternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = this.orb.resolve_initial_references("RootPOA");
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o RootPOA";
      logger.log(Level.SEVERE, message, e);
      throw new OpenBusInternalException(message, e);
    }
    return POAHelper.narrow(obj);
  }

  @Override
  public void activateRootPOAManager() throws AdapterInactive {
    POA poa = getRootPOA();
    POAManager manager = poa.the_POAManager();
    manager.activate();
  }

  @Override
  public Codec getCodec() {
    return this.mediator.getCodec();
  }
}
