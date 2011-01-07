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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tecgraf.openbus.FaultToleranceManager;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_06.registry_service.IRegistryService;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;
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

  private long start;

  /**
   * Gerencia a lista de replicas.
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
    Logger logger = LoggerFactory.getLogger(ClientInterceptor.class);
    logger.debug("Interceptador criado");
  }

  @Override
  public void receive_reply(ClientRequestInfo ri) {
    Logger loadTestLogger =
      LoggerFactory
        .getLogger("LoadTest." + FTClientInterceptor.class.getName());

    loadTestLogger.info(
      "--; {}; {}; {}",
      new Object[] { (System.currentTimeMillis() - start), getObjectKey(ri),
          ri.operation() });

    //se entrou aqui é porque a chamada remota retornou sem erro, 
    //logo deve reiniciar a variável de controle de tentativas de 
    //buscar por uma réplica válida
    ftManager.resetCurrTrial();
  }

  @Override
  public void send_request(ClientRequestInfo ri) {
    Logger loadTestLogger =
      LoggerFactory.getLogger("LoadTest." + ClientInterceptor.class.getName());

    String key = getObjectKey(ri);

    start = System.currentTimeMillis();
    loadTestLogger.info("inicio; 0; {}; {}",
      new Object[] { key, ri.operation() });

    super.send_request(ri);

    loadTestLogger
      .info("fim; {}; {}; {}", new Object[] {
          (System.currentTimeMillis() - start), key, ri.operation() });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
    Logger logger = LoggerFactory.getLogger(ClientInterceptor.class);
    logger.debug("Tratando exceção enviada do servidor: {}",
      ri.received_exception_id());

    Logger loadTestLogger =
      LoggerFactory.getLogger("LoadTest." + ClientInterceptor.class.getName());

    String key = getObjectKey(ri);
    loadTestLogger.error("inicio; {}; {}; {}; {}",
      new Object[] { (System.currentTimeMillis() - start), key, ri.operation(),
          ri.received_exception_id() });

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
      logger.error(ri.received_exception_id());
      return;
    }

    Openbus bus = Openbus.getInstance();

    String msg = "";
    if (key.equals(Utils.OPENBUS_KEY) || key.equals(ACCESS_CONTROL_SERVICE_KEY)
      || key.equals(LEASE_PROVIDER_KEY) || key.equals(FAULT_TOLERANT_ACS_KEY)) {
      while (fetch) {
        if (ftManager.updateACSHostInUse()) {
          bus.setHost(ftManager.getACSHostInUse().getHostName());
          bus.setPort(ftManager.getACSHostInUse().getHostPort());
          try {
            bus.fetchACS();

            loadTestLogger.error(
              "fim; {}; {}; {}; {}",
              new Object[] { (System.currentTimeMillis() - start), key,
                  ri.operation(), ri.received_exception_id() });

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
      loadTestLogger.error(
        "fim; {}; {}; {}; {}",
        new Object[] { (System.currentTimeMillis() - start), key,
            ri.operation(), ri.received_exception_id() });

      logger.debug("[ACSUnavailableException] {}", msg);
    }
    else if (key.equals(REGISTRY_SERVICE_KEY)) {
      IRegistryService rs = bus.getRegistryService();

      loadTestLogger.error(
        "fim; {}; {}; {}; {}",
        new Object[] { (System.currentTimeMillis() - start), key,
            ri.operation(), ri.received_exception_id() });

      throw new ForwardRequest(rs);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_other(ClientRequestInfo ri) {
    Logger logger = LoggerFactory.getLogger(ClientInterceptor.class);
    logger.debug("Tratando outra resposta.");

    Logger loadTestLogger =
      LoggerFactory.getLogger("LoadTest." + ClientInterceptor.class.getName());

    loadTestLogger.error(
      "--; {}; {}; {}",
      new Object[] { (System.currentTimeMillis() - start), getObjectKey(ri),
          ri.operation() });
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
