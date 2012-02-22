package tecgraf.openbus;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;

public interface Bus {
  BusORB getORB();

  String getId();

  RSAPublicKey getPublicKey();

  Connection createConnection() throws CryptographyException;

  Collection<Connection> getConnections();
}
