/*
 * $Id: FTClientInterceptor.java
 */
package tecgraf.openbus.interceptors;

import org.jacorb.orb.ParsedIOR;
import org.jacorb.orb.util.CorbaLoc;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ForwardRequest;

import tecgraf.openbus.FaultToleranceManager;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.registry_service.IRegistryService;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;
import tecgraf.openbus.util.Log;
import tecgraf.openbus.util.Utils;

/**
 * Implementa um interceptador "cliente" com tolerância a falhas
 * 
 * @author Tecgraf/PUC-Rio
 */
class FTClientInterceptor extends ClientInterceptor {
  private static final String ACCESS_CONTROL_SERVICE_KEY = "ACS_v1_05";
  private static final String LEASE_PROVIDER_KEY = "LP_v1_05";
  private static final String FAULT_TOLERANT_ACS_KEY = "FTACS_v1_05";
  private static final String REGISTRY_SERVICE_KEY = "RS_v1_05";

  /**
   *Gerencia a lista de replicas.
   */
  private FaultToleranceManager ftManager;

  /**
   * Constrói o interceptador.
   * 
   * @param codec codificador/decodificador
   */
  FTClientInterceptor(Codec codec) {
    super(codec);
    this.ftManager = FaultToleranceManager.getInstance();
    Log.INTERCEPTORS.info("[FTClientInterceptor] INTERCEPTADOR CRIADO!");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    Log.INTERCEPTORS
      .info("[receive_exception] TRATANDO EXCECAO ENVIADA DO SERVIDOR!");

    String msg = "";
    boolean fetch =
      ri.received_exception_id().equals("IDL:omg.org/CORBA/TRANSIENT:1.0") ||
      ri.received_exception_id()
        .equals("IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0") ||
      ri.received_exception_id().equals("IDL:omg.org/CORBA/COMM_FAILURE:1.0");

    if (!fetch) return;

    Openbus bus = Openbus.getInstance();
    ORB orb = bus.getORB();

    ParsedIOR pior =
      new ParsedIOR((org.jacorb.orb.ORB) orb, orb.object_to_string(ri.target()));
    String key = CorbaLoc.parseKey(pior.get_object_key());

    if (key.equals(Utils.OPENBUS_KEY) || key.equals(ACCESS_CONTROL_SERVICE_KEY)
      || key.equals(LEASE_PROVIDER_KEY) || key.equals(FAULT_TOLERANT_ACS_KEY)) {
      while (fetch) {
        if (ftManager.updateACSHostInUse()) {
          bus.setHost(ftManager.getACSHostInUse().getHostName());
          bus.setPort(ftManager.getACSHostInUse().getHostPort());
          try {
            bus.fetchACS();
            if (key.equals(ACCESS_CONTROL_SERVICE_KEY)) {
              throw new ForwardRequest(bus.getAccessControlService());
            }
            else if (key.equals(LEASE_PROVIDER_KEY)) {
              throw new ForwardRequest(bus.getLeaseProvider());
            }
            else if (key.equals(Utils.OPENBUS_KEY)) {
              throw new ForwardRequest(bus.getACSIComponent());
            }
            else if (key.equals(FAULT_TOLERANT_ACS_KEY)) {
              throw new ForwardRequest(bus.getACSFaultTolerantService());
            }
          }
          catch (ACSUnavailableException e) {
            fetch = true;
            msg = e.getMessage();
          }
          catch (CORBAException e) {
            fetch = true;
            msg = e.getMessage();
          }
        }
        else {
          fetch = false;
        }
      }
      System.out.println("[ACSUnavailableException] " + msg);
    }
    else if (key.equals(REGISTRY_SERVICE_KEY)) {
      IRegistryService rs = bus.getRegistryService();
      throw new ForwardRequest(rs);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_other(ClientRequestInfo ri) {
    Log.INTERCEPTORS.info("[receive_other] TRATANDO OUTRA RESPOSTA!");
    // TODO
  }
}
