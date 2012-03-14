package tecgraf.openbus;

public interface ConnectionMultiplexer {
  Connection[] getConnections();

  void setCurrentConnection(Connection conn);

  Connection getCurrentConnection();

  void setIncommingConnection(String busid, Connection conn);

  Connection getIncommingConnection(String busid);
}
