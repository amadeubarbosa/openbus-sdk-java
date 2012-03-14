package tecgraf.openbus;

import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.PortableServer.POA;

import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.exception.OpenBusInternalException;

public interface BusORB {
  Bus getBus(String host, int port) throws CryptographyException;

  Bus hasBus(String busid);

  Connection getCurrentConnection();

  void setCurrentConnection(Connection connection);

  CallerChain getCallerChain() throws OpenBusInternalException;

  void ignoreCurrentThread();

  void unignoreCurrentThread();

  boolean isCurrentThreadIgnored();

  ORB getORB();

  POA getRootPOA() throws OpenBusInternalException;

  Codec getCodec();
}
