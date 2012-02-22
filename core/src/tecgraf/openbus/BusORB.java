package tecgraf.openbus;

import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.PortableServer.POA;

public interface BusORB {
  Bus getBus(String host, int port) throws CryptographyException;

  Connection getCurrentConnection();

  void setCurrentConnection(Connection connection);

  CallerChain getCallerChain() throws InternalException;

  void ignoreCurrentThread();

  void unignoreCurrentThread();

  boolean isCurrentThreadIgnored();

  ORB getORB();

  POA getRootPOA() throws InternalException;

  Codec getCodec();
}
