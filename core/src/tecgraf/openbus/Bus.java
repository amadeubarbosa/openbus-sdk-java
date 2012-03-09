package tecgraf.openbus;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;

import tecgraf.openbus.exception.CryptographyException;

public interface Bus {
  BusORB getORB();

  String getId();

  RSAPublicKey getPublicKey();

  Connection createConnection() throws CryptographyException;

  Collection<Connection> getConnections();
}
