package tecgraf.openbus.core;

import java.util.Properties;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBus;

/**
 * Implementa��o do SDK sem uso do mecanismo de multiplexa��o.
 * 
 * @author Tecgraf
 */
public class StandardOpenBus extends OpenBus {

  /**
   * A refer�ncia.
   */
  private static StandardOpenBus instance;

  /**
   * Recupera uma inst�ncia do SDK sem o mencanismo de multiplexa��o.
   * 
   * @return a inst�ncia do SDK.
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
      // CHECK verificar se a restri��o seria por barramento e a exce��o a lan�ar
      throw new IllegalStateException(
        "N�o � poss�vel existir mais de uma conex�o.");
    }
    return super.connect(host, port, orb);
  }
}