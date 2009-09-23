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

import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.exception.ACSUnavailableException;

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
  public static final String LEASE_PROVIDER_KEY = "LP";
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
  private static org.omg.CORBA.Object fetchACS(ORB orb, String host, int port,
    String key) throws ACSUnavailableException {
    org.omg.CORBA.Object obj =
      orb.string_to_object("corbaloc::1.0@" + host + ":" + port + "/" + key);
    if (obj == null) {
      throw new ACSUnavailableException();
    }
    try {
      if (obj._non_existent()) {
        throw new ACSUnavailableException();
      }
    }
    catch (OBJECT_NOT_EXIST e) {
      throw new ACSUnavailableException();
    }
    return obj;
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
   */
  public static IAccessControlService fetchAccessControlService(ORB orb,
    String host, int port) throws ACSUnavailableException {
    org.omg.CORBA.Object obj =
      fetchACS(orb, host, port, ACCESS_CONTROL_SERVICE_KEY);
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
   */
  public static IComponent fetchAccessControlServiceIComponent(ORB orb,
    String host, int port) throws ACSUnavailableException {
    org.omg.CORBA.Object obj = fetchACS(orb, host, port, ICOMPONENT_KEY);
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
   */
  public static ILeaseProvider fetchAccessControlServiceLeaseProvider(ORB orb,
    String host, int port) throws ACSUnavailableException {
    org.omg.CORBA.Object obj = fetchACS(orb, host, port, LEASE_PROVIDER_KEY);
    return ILeaseProviderHelper.narrow(obj);
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
