package tecgraf.openbus.core;

import java.util.Properties;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.OpenBus;

/**
 * Implementação do SDK com o mecanismo de multiplexação habilitado.
 * 
 * @author Tecgraf
 */
public class MultiplexedOpenBus extends OpenBus {

  /**
   * A referência.
   */
  private static MultiplexedOpenBus instance;

  /**
   * Recupera uma instância do SDK com o mencanismo de multiplexação habilitado.
   * 
   * @return a instância do SDK.
   */
  public static OpenBus getInstance() {
    if (instance == null) {
      instance = new MultiplexedOpenBus();
    }
    return instance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BusORB initORB(String[] args, Properties props) {
    BusORBImpl orb = (BusORBImpl) super.initORB(args, props);
    orb.getConnectionMultiplexer().isMultiplexed(true);
    return orb;
  }

}
