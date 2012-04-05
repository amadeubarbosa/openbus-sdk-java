package tecgraf.openbus.core;

import java.util.Properties;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.BusORB;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBus;
import tecgraf.openbus.core.v2_00.BusObjectKey;

/**
 * Parte comum entre a API com multiplexação e a sem multiplexação.
 * 
 * @author Tecgraf
 */
abstract class OpenBusImpl implements OpenBus {

  /**
   * {@inheritDoc}
   */
  @Override
  public BusORB initORB() {
    return this.initORB(null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BusORB initORB(String[] args) {
    return this.initORB(args, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract BusORB initORB(String[] args, Properties props);

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection connect(String host, int port) {
    return connect(host, port, initORB());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection connect(String host, int port, BusORB orb) {
    ((BusORBImpl) orb).ignoreCurrentThread();
    try {
      String str =
        String.format("corbaloc::1.0@%s:%d/%s", host, port, BusObjectKey.value);
      org.omg.CORBA.Object obj = orb.getORB().string_to_object(str);
      if (obj == null) {
        return null;
      }
      IComponent component = IComponentHelper.narrow(obj);
      BusInfo bus = new BusInfo(component);
      Connection conn = new ConnectionImpl(bus, orb);
      return conn;
    }
    finally {
      ((BusORBImpl) orb).unignoreCurrentThread();
    }
  }

}
