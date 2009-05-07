/*
 * $Id$
 */
package openbus.common;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import openbus.ORBWrapper;
import openbus.RegistryServiceWrapper;
import openbus.common.exception.ACSUnavailableException;
import openbus.exception.CORBAException;
import openbusidl.data_service.IHDataService;
import openbusidl.data_service.IHDataServiceHelper;
import openbusidl.rs.Property;
import openbusidl.rs.ServiceOffer;
import openbusidl.ss.ISessionService;
import openbusidl.ss.ISessionServiceHelper;

import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;

import scs.core.ComponentId;
import scs.core.IComponent;
import scs.core.IComponentHelper;

/**
 * Métodos utilitários para uso do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Utils {
  /**
   * Representa a interface do serviço de controle de acesso.
   */
  public static final String ACCESS_CONTROL_SERVICE_INTERFACE =
    "IDL:openbusidl/acs/IAccessControlService:1.0";
  /**
   * Representa a interface lease provider.
   */
  public static final String LEASE_PROVIDER_INTERFACE =
    "IDL:openbusidl/acs/ILeaseProvider:1.0";
  /**
   * Representa a interface do serviço de sessão.
   */
  public static final String SESSION_SERVICE_INTERFACE =
    "IDL:openbusidl/ss/ISessionService:1.0";
  /**
   * O nome da faceta do Serviço de Sessão.
   */
  private static final String SESSION_SERVICE_FACET_NAME = "sessionService";
  /**
   * O nome da propriedade que representa as facetas de um membro registrado.
   */
  private static final String FACETS_PROPERTY_NAME = "facets";
  /**
   * Representa a interface do serviço de Dddos.
   */
  public static final String DATA_SERVICE_INTERFACE =
    "IDL:openbusidl/data_service/IHDataService:1.0";
  /**
   * Nome da propriedade que indica o identificador de um componente.
   */
  public static final String COMPONENT_ID_PROPERTY_NAME = "component_id";

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
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public static IComponent fetchAccessControlServiceIComponent(ORBWrapper orb,
    String host, int port) throws ACSUnavailableException, CORBAException {
    try {
      org.omg.CORBA.Object obj =
        orb.getORB().string_to_object(
          "corbaloc::1.0@" + host + ":" + port + "/IC");
      if ((obj == null) || (obj._non_existent())) {
        throw new ACSUnavailableException();
      }
      return IComponentHelper.narrow(obj);
    }
    catch (SystemException e) {
      e.printStackTrace();
      throw new CORBAException(e);
    }
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

  /**
   * Obtém o serviço de sessão.
   * 
   * @param registryService O serviço de registro.
   * 
   * @return O serviço de sessão, ou {@code null}, caso não seja encontrado.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public static ISessionService getSessionService(
    RegistryServiceWrapper registryService) throws CORBAException {
    Property[] properties = new openbusidl.rs.Property[1];
    properties[0] =
      new Property(FACETS_PROPERTY_NAME,
        new String[] { SESSION_SERVICE_FACET_NAME });
    try {
      ServiceOffer[] offers = registryService.find(properties);
      if (offers.length == 1) {
        IComponent component = offers[0].member;
        Object facet = component.getFacet(SESSION_SERVICE_INTERFACE);
        if (facet == null) {
          return null;
        }
        return ISessionServiceHelper.narrow(facet);
      }
      return null;
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
  }

  /**
   * Obtém um serviço de dados.
   * 
   * @param registryService O serviço de registro.
   * @param dataServiceId O identificador do Serviço de Dados.
   * 
   * @return O serviço de dados, ou {@code null}, caso não seja encontrado.
   * 
   * @throws CORBAException Caso ocorra alguma exceção na infra-estrutura CORBA.
   */
  public static IHDataService getDataService(
    RegistryServiceWrapper registryService, ComponentId dataServiceId)
    throws CORBAException {
    Property[] properties = new Property[1];
    properties[0] =
      new Property(COMPONENT_ID_PROPERTY_NAME,
        new String[] { dataServiceId.name + ":" + dataServiceId.major_version
          + dataServiceId.minor_version + dataServiceId.patch_version });
    try {
      ServiceOffer[] offers = registryService.find(properties);
      if (offers.length == 1) {
        IComponent dataServiceComponent = offers[0].member;
        Object dataServiceFacet =
          dataServiceComponent.getFacet(DATA_SERVICE_INTERFACE);
        if (dataServiceFacet == null) {
          return null;
        }
        return IHDataServiceHelper.narrow(dataServiceFacet);
      }
      return null;
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
  }
}
