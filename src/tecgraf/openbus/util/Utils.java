/*
 * $Id$
 */
package tecgraf.openbus.util;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import openbusidl.acs.IAccessControlService;
import openbusidl.acs.IAccessControlServiceHelper;
import openbusidl.acs.ILeaseProvider;
import openbusidl.acs.ILeaseProviderHelper;
import openbusidl.ft.IFaultTolerantService;
import openbusidl.ft.IFaultTolerantServiceHelper;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TRANSIENT;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;
import tecgraf.openbus.exception.ServiceUnavailableException;

/**
 * Métodos utilitários para uso do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Utils {
  /**
   * Representam as chaves CORBALOC para obtenção das interfaces do ACS.
   */
  public static final String ICOMPONENT_KEY = "IC";
  public static final String ACCESS_CONTROL_SERVICE_KEY = "ACS";
  public static final String REGISTRY_SERVICE_KEY = "RS";
  public static final String LEASE_PROVIDER_KEY = "LP";
  public static final String FAULT_TOLERANT_KEY = "FT";

  /**
   * O nome da faceta do Serviço de Sessão.
   */
  public static final String SESSION_SERVICE_FACET_NAME = "ISessionService";
  /**
   * O nome da propriedade que representa as facetas de um membro registrado.
   */
  public static final String FACETS_PROPERTY_NAME = "facets";
  /**
   * Nome da propriedade que indica o identificador de um componente.
   */
  public static final String COMPONENT_ID_PROPERTY_NAME = "component_id";

  /**
   * Método privado para facilitar a obtenção do serviço de controle de acesso.
   */
  private static org.omg.CORBA.Object fetchService(ORB orb, String host,
    int port, String key) throws ServiceUnavailableException {
    org.omg.CORBA.Object obj =
      orb.string_to_object("corbaloc::1.0@" + host + ":" + port + "/" + key);
    return obj;
  }

  private static void checkACS(org.omg.CORBA.Object acs)
    throws ACSUnavailableException, CORBAException {
    if (acs == null) {
      throw new ACSUnavailableException();
    }
    try {
      if (acs._non_existent()) {
        throw new ACSUnavailableException();
      }
    }
    catch (TRANSIENT te) {
      throw new ACSUnavailableException();
    }
    catch (OBJECT_NOT_EXIST e) {
      throw new ACSUnavailableException();
    }
    catch (COMM_FAILURE ce) {
      throw new ACSUnavailableException();
    }
    catch (SystemException e) {
      e.printStackTrace();
      throw new CORBAException(e);
    }
  }

  /**
   * Obtém o serviço de controle de acesso.
   * 
   * @param orb O orb utilizado para obter o serviço.
   * @param host A máquina onde o serviço está localizado.
   * @param port A porta onde o serviço está disponível.
   * 
   * @return O serviço de controle de acesso.
   * 
   * @throws ACSUnavailableException Caso o serviço não seja encontrado.
   * @throws CORBAException
   */
  public static IAccessControlService fetchAccessControlService(ORB orb,
    String host, int port) throws ServiceUnavailableException, CORBAException {
    org.omg.CORBA.Object obj =
      fetchService(orb, host, port, ACCESS_CONTROL_SERVICE_KEY);
    checkACS(obj);
    return IAccessControlServiceHelper.narrow(obj);
  }

  /**
   * Obtém o IComponent do serviço de controle de acesso.
   * 
   * @param orb O orb utilizado para obter o serviço.
   * @param host A máquina onde o serviço está localizado.
   * @param port A porta onde o serviço está disponível.
   * 
   * @return O IComponent do serviço de controle de acesso.
   * 
   * @throws ACSUnavailableException Caso o serviço não seja encontrado.
   * @throws CORBAException
   */
  public static IComponent fetchAccessControlServiceIComponent(ORB orb,
    String host, int port) throws ServiceUnavailableException, CORBAException {
    org.omg.CORBA.Object obj = fetchService(orb, host, port, ICOMPONENT_KEY);
    checkACS(obj);
    return IComponentHelper.narrow(obj);
  }

  /**
   * Obtém o LeaseProvider do serviço de controle de acesso.
   * 
   * @param orb O orb utilizado para obter o serviço.
   * @param host A máquina onde o serviço está localizado.
   * @param port A porta onde o serviço está disponível.
   * 
   * @return O LeaseProvider do serviço de controle de acesso.
   * 
   * @throws ACSUnavailableException Caso o serviço não seja encontrado.
   * @throws CORBAException
   */
  public static ILeaseProvider fetchAccessControlServiceLeaseProvider(ORB orb,
    String host, int port) throws ServiceUnavailableException, CORBAException {
    org.omg.CORBA.Object obj =
      fetchService(orb, host, port, LEASE_PROVIDER_KEY);
    checkACS(obj);
    return ILeaseProviderHelper.narrow(obj);
  }

  /**
   * Obtém a faceta IFaultTolerantService do serviço de controle de acesso.
   * 
   * @param orb O orb utilizado para obter o serviço.
   * @param host A máquina onde o serviço está localizado.
   * @param port A porta onde o serviço está disponível.
   * 
   * @return A faceta de tolerancia a falhas do serviço de controle de acesso.
   * 
   * @throws ServiceUnavailableException Caso o serviço não seja encontrado.
   * @throws CORBAException
   */
  public static IFaultTolerantService fetchAccessControlServiceFaultTolerant(
    ORB orb, String host, int port) throws ServiceUnavailableException,
    CORBAException {
    org.omg.CORBA.Object obj =
      fetchService(orb, host, port, FAULT_TOLERANT_KEY + "ACS");
    checkACS(obj);
    return IFaultTolerantServiceHelper.narrow(obj);
  }

  /**
   * Gera a resposta para o desafio gerado pelo serviço de controle de acesso.
   * 
   * @param challenge O desafio.
   * @param privateKey A chave privada de quem está respondendo ao desafio.
   * @param acsCertificate O certificado do serviço de controle de acesso
   * 
   * @return A resposta para o desafio.
   * 
   * @throws GeneralSecurityException Caso ocorra algum problema durante a
   *         operação.
   */
  public static byte[] generateAnswer(byte[] challenge, PrivateKey privateKey,
    Certificate acsCertificate) throws GeneralSecurityException {
    byte[] plainChallenge = CryptoUtils.decrypt(privateKey, challenge);
    return CryptoUtils.encrypt(acsCertificate, plainChallenge);
  }
}
