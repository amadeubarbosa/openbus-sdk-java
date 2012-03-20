package tecgraf.openbus;

import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import tecgraf.openbus.exception.OpenBusInternalException;

public interface BusORB {

  CallerChain getCallerChain() throws OpenBusInternalException;

  ORB getORB();

  POA getRootPOA() throws OpenBusInternalException;

  void activateRootPOAManager() throws AdapterInactive;

  Codec getCodec();

  void ignoreCurrentThread();

  void unignoreCurrentThread();

  boolean isCurrentThreadIgnored();

}
