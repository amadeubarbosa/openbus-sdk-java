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
import tecgraf.openbus.util.LoadLog;
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
   * O log dos testes de carga.
   */
  public static final LoadLog LOAD_TEST = new LoadLog("openbus.loadtest");

  private long start;

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

  @Override
  public void receive_reply(ClientRequestInfo ri) {
    String key = getObjectKey(ri);
    String loadMsg =
      "receive_reply; --; ; " + (System.currentTimeMillis() - start) + "; "
        + key + "; " + ri.operation();
    LOAD_TEST.info(loadMsg);

    // se entrou aqui é porque a chamada remota retornou sem erro,
    // logo deve reiniciar a variável de controle de tentativas de
    // buscar por uma réplica válida
    ftManager.resetCurrTrial();
  }

  @Override
  public void send_request(ClientRequestInfo ri) {
    String key = getObjectKey(ri);

    start = System.currentTimeMillis();
    String loadMsg = "send_request; inicio; 0; " + key + "; " + ri.operation();
    LOAD_TEST.info(loadMsg);

    super.send_request(ri);

    loadMsg =
      "send_request; fim; ; " + (System.currentTimeMillis() - start) + "; "
        + key + "; " + ri.operation();
    LOAD_TEST.info(loadMsg);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    Log.INTERCEPTORS
      .fine("[receive_exception] TRATANDO EXCECAO ENVIADA DO SERVIDOR: "
        + ri.received_exception_id());

    String key = getObjectKey(ri);
    String loadMsg =
      "receive_exception; inicio; ; " + (System.currentTimeMillis() - start)
        + "; " + key + "; " + ri.operation() + "; "
        + ri.received_exception_id();
    LOAD_TEST.severe(loadMsg);

    String msg = "";
    boolean fetch =
      ri.received_exception_id().equals("IDL:omg.org/CORBA/TRANSIENT:1.0")
        || ri.received_exception_id().equals(
          "IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0")
        || ri.received_exception_id().equals(
          "IDL:omg.org/CORBA/COMM_FAILURE:1.0")
        || ri.received_exception_id().equals("IDL:omg.org/CORBA/TIMEOUT:1.0")
        || ri.received_exception_id().equals(
          "IDL:omg.org/CORBA/NO_RESPONSE:1.0")
        || ri.received_exception_id().equals(
          "IDL:omg.org/CORBA/NO_RESOURCES:1.0")
        || ri.received_exception_id().equals("IDL:omg.org/CORBA/NO_MEMORY:1.0")
        || ri.received_exception_id().equals("IDL:omg.org/CORBA/INTERNAL:1.0");

    if (!fetch) {
      Log.INTERCEPTORS.severe(ri.received_exception_id());
      return;
    }

    Openbus bus = Openbus.getInstance();

    if (key.equals(Utils.OPENBUS_KEY) || key.equals(ACCESS_CONTROL_SERVICE_KEY)
      || key.equals(LEASE_PROVIDER_KEY) || key.equals(FAULT_TOLERANT_ACS_KEY)) {
      while (fetch) {
        if (ftManager.updateACSHostInUse()) {
          bus.setHost(ftManager.getACSHostInUse().getHostName());
          bus.setPort(ftManager.getACSHostInUse().getHostPort());
          try {
            bus.fetchACS();

            loadMsg =
              "receive_exception; fim; ; "
                + (System.currentTimeMillis() - start) + "; " + key + "; "
                + ri.operation() + "; " + ri.received_exception_id();
            LOAD_TEST.severe(loadMsg);

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
      loadMsg =
        "receive_exception; fim; ; " + (System.currentTimeMillis() - start)
          + "; " + key + "; " + ri.operation() + "; "
          + ri.received_exception_id();
      LOAD_TEST.severe(loadMsg);

      Log.INTERCEPTORS.info("[receive_exception][ACSUnavailableException] "
        + msg);
    }
    else if (key.equals(REGISTRY_SERVICE_KEY)) {
      IRegistryService rs = bus.getRegistryService();

      loadMsg =
        "receive_exception; fim; ; " + (System.currentTimeMillis() - start)
          + "; " + key + "; " + ri.operation() + "; "
          + ri.received_exception_id();
      LOAD_TEST.severe(loadMsg);

      throw new ForwardRequest(rs);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_other(ClientRequestInfo ri) {
    Log.INTERCEPTORS.fine("[receive_other] TRATANDO OUTRA RESPOSTA!");

    String key = getObjectKey(ri);
    String loadMsg =
      "receive_other; --; ; " + (System.currentTimeMillis() - start) + "; "
        + key + "; " + ri.operation();
    LOAD_TEST.severe(loadMsg);
  }

  public String getObjectKey(ClientRequestInfo ri) {
    Openbus bus = Openbus.getInstance();
    ORB orb = bus.getORB();
    ParsedIOR pior =
      new ParsedIOR((org.jacorb.orb.ORB) orb, orb.object_to_string(ri.target()));
    String key = CorbaLoc.parseKey(pior.get_object_key());
    return key;
  }

}
