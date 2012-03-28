package tecgraf.openbus.core;

import java.util.Properties;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBus;

/**
 * Implementação do SDK sem uso do mecanismo de multiplexação.
 * 
 * @author Tecgraf
 */
public class StandardOpenBus extends OpenBus {

  /**
   * A referência.
   */
  private static StandardOpenBus instance;

  /**
   * Recupera uma instância do SDK sem o mencanismo de multiplexação.
   * 
   * @return a instância do SDK.
   */
  public static synchronized OpenBus getInstance() {
    if (instance == null) {
      instance = new StandardOpenBus();
    }
    return instance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BusORB initORB(String[] args, Properties props) {
    BusORBImpl orb = (BusORBImpl) super.initORB(args, props);
    orb.getConnectionMultiplexer().isMultiplexed(false);
    return orb;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection connect(String host, int port, BusORB orb) {
    BusORBImpl busOrb = (BusORBImpl) orb;
    ConnectionMultiplexerImpl multi = busOrb.getConnectionMultiplexer();
    if (!multi.isMultiplexed() && multi.getConnections().length > 0) {
      // CHECK verificar se a restrição seria por barramento e a exceção a lançar
      throw new IllegalStateException(
        "Não é possível existir mais de uma conexão.");
    }
    return super.connect(host, port, orb);
  }
}