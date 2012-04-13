package tecgraf.openbus.core;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;

import tecgraf.openbus.ConnectionManager;
import tecgraf.openbus.exception.OpenBusInternalException;

public class ORBUtils {

  static Current getPICurrent(ORB orb) throws OpenBusInternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = orb.resolve_initial_references("PICurrent");
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o PICurrent";
      throw new OpenBusInternalException(message, e);
    }
    return CurrentHelper.narrow(obj);
  }

  static ORBMediator getMediator(ORB orb) throws OpenBusInternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = orb.resolve_initial_references(ORBMediator.INITIAL_REFERENCE_ID);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o mediador";
      throw new OpenBusInternalException(message, e);
    }
    return (ORBMediator) obj;
  }

  static ConnectionManagerImpl getConnectionManager(ORB orb) {
    org.omg.CORBA.Object obj;
    try {
      obj =
        orb.resolve_initial_references(ConnectionManager.INITIAL_REFERENCE_ID);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o multiplexador";
      throw new OpenBusInternalException(message, e);
    }
    return (ConnectionManagerImpl) obj;
  }
}
