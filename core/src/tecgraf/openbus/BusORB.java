package tecgraf.openbus;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import tecgraf.openbus.exception.OpenBusInternalException;

/**
 * Classe que engloba o {@link ORB} de CORBA para uso no SDK.
 * 
 * @author Tecgraf
 */
public interface BusORB {

  /**
   * Recupera o {@link ORB} de CORBA encapsulado.
   * 
   * @return o ORB
   */
  ORB getORB();

  /**
   * Recupear o RootPOA do {@link ORB}
   * 
   * @return o RootPOA
   * @throws OpenBusInternalException
   */
  POA getRootPOA() throws OpenBusInternalException;

  /**
   * Ativa o POA Manager do ORB para aceitar chamadas remotas.
   * 
   * @throws AdapterInactive
   */
  void activateRootPOAManager() throws AdapterInactive;

}
