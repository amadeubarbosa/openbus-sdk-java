/*
 * $Id$
 */
package openbus.common;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import openbus.common.exception.ACSUnavailableException;
import openbusidl.acs.IAccessControlService;
import openbusidl.acs.IAccessControlServiceHelper;
import openbusidl.ds.IDataService;
import openbusidl.ds.IDataServiceHelper;
import openbusidl.rs.IRegistryService;
import openbusidl.rs.Property;
import openbusidl.rs.ServiceOffer;
import openbusidl.ss.ISessionService;
import openbusidl.ss.ISessionServiceHelper;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.TRANSIENT;

import scs.core.ComponentId;
import scs.core.IComponent;

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
    "IDL:openbusidl/ds/IDataService:1.0";
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
   */
  public static IAccessControlService fetchAccessControlService(ORB orb,
    String host, int port) throws ACSUnavailableException {
    String url = "corbaloc::1.0@" + host + ":" + port + "/ACS";
    org.omg.CORBA.Object obj = orb.string_to_object(url);
    try {
      if (obj._non_existent()) {
        throw new ACSUnavailableException(
          "Serviço de Controle de Acesso não disponível.");
      }
    }
    catch (TRANSIENT e) {
      Log.COMMON.severe("Falha no acesso ao serviço de controle de acesso.", e);
      throw new ACSUnavailableException(
        "Serviço de Controle de Acesso não disponível.");
    }
    return IAccessControlServiceHelper.narrow(obj);
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
   */
  public static ISessionService getSessionService(
    IRegistryService registryService) {
    Property[] properties = new openbusidl.rs.Property[1];
    properties[0] =
      new Property(FACETS_PROPERTY_NAME,
        new String[] { SESSION_SERVICE_FACET_NAME });
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

  /**
   * Obtém um serviço de dados.
   * 
   * @param registryService O serviço de registro.
   * @param dataServiceId O identificador do Serviço de Dados.
   * 
   * @return O serviço de dados, ou {@code null}, caso não seja encontrado.
   */
  public static IDataService getDataService(IRegistryService registryService,
    ComponentId dataServiceId) {
    Property[] properties = new Property[1];
    properties[0] =
      new Property(COMPONENT_ID_PROPERTY_NAME,
        new String[] { dataServiceId.name + ":" + dataServiceId.version });
    ServiceOffer[] offers = registryService.find(properties);
    if (offers.length == 1) {
      IComponent dataServiceComponent = offers[0].member;
      Object dataServiceFacet =
        dataServiceComponent.getFacet(DATA_SERVICE_INTERFACE);
      if (dataServiceFacet == null) {
        return null;
      }
      return IDataServiceHelper.narrow(dataServiceFacet);
    }
    return null;
  }
}
