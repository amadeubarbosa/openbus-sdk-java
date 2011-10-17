package tecgraf.openbus;

import java.util.Collection;

public interface Bus {
  BusORB getORB();

  String getId();

  Connection createConnection() throws CryptographyException;

  Collection<Connection> getConnections();
}
