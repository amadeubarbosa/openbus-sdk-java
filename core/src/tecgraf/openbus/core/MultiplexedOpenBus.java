package tecgraf.openbus.core;

import java.util.Properties;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.OpenBus;

/**
 * Implementa��o do SDK com o mecanismo de multiplexa��o habilitado.
 * 
 * @author Tecgraf
 */
public class MultiplexedOpenBus extends OpenBus {

  /**
   * A refer�ncia.
   */
  private static MultiplexedOpenBus instance;

  /**
   * Recupera uma inst�ncia do SDK com o mencanismo de multiplexa��o habilitado.
   * 
   * @return a inst�ncia do SDK.
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
