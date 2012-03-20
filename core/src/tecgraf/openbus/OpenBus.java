package tecgraf.openbus;

import java.util.Properties;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.core.BusInfo;
import tecgraf.openbus.core.BusORBImpl;
import tecgraf.openbus.core.ConnectionImpl;
import tecgraf.openbus.core.v2_00.BusObjectKey;

public class OpenBus {

  static public BusORB initORB() {
    return new BusORBImpl();
  }

  static public BusORB initORB(String[] args) {
    return new BusORBImpl(args, null);
  }

  static public BusORB initORB(String[] args, Properties props) {
    return new BusORBImpl(args, props);
  }

  static public Connection connect(String host, int port) {
    return connect(host, port, initORB());
  }

  static public Connection connect(String host, int port, BusORB orb) {
    orb.ignoreCurrentThread();
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
      orb.unignoreCurrentThread();
    }
  }

}
