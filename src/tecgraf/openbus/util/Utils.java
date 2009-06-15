/*
 * $Id$
 */
package tecgraf.openbus.util;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import openbusidl.rs.IRegistryService;
import openbusidl.rs.Property;
import openbusidl.rs.ServiceOffer;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;

import scs.core.ComponentId;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.corba.data_service.IHDataService;
import tecgraf.openbus.corba.data_service.IHDataServiceHelper;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;

/**
 * M�todos utilit�rios para uso do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Utils {
  /**
   * Representa a interface do servi�o de controle de acesso.
   */
  public static final String ACCESS_CONTROL_SERVICE_INTERFACE =
    "IDL:openbusidl/acs/IAccessControlService:1.0";
  /**
   * Representa a interface lease provider.
   */
  public static final String LEASE_PROVIDER_INTERFACE =
    "IDL:openbusidl/acs/ILeaseProvider:1.0";
  /**
   * Representa a interface do servi�o de sess�o.
   */
  public static final String SESSION_SERVICE_INTERFACE =
    "IDL:openbusidl/ss/ISessionService:1.0";
  /**
   * O nome da faceta do Servi�o de Sess�o.
   */
  public static final String SESSION_SERVICE_FACET_NAME = "sessionService";
  /**
   * O nome da propriedade que representa as facetas de um membro registrado.
   */
  public static final String FACETS_PROPERTY_NAME = "facets";
  /**
   * Representa a interface do servi�o de Dddos.
   */
  public static final String DATA_SERVICE_INTERFACE =
    "IDL:openbusidl/data_service/IHDataService:1.0";
  /**
   * Nome da propriedade que indica o identificador de um componente.
   */
  public static final String COMPONENT_ID_PROPERTY_NAME = "component_id";

  /**
   * Obt�m o servi�o de controle de acesso.
   * 
   * @param orb O orb utilizado para obter o servi�o.
   * @param host A m�quina onde o servi�o est� localizado.
   * @param port A porta onde o servi�o est� dispon�vel.
   * 
   * @return O servi�o de controle de acesso.
   * 
   * @throws ACSUnavailableException Caso o servi�o n�o seja encontrado.
   */
  public static IComponent fetchAccessControlServiceIComponent(ORB orb,
    String host, int port) throws ACSUnavailableException {
    org.omg.CORBA.Object obj =
      orb.string_to_object("corbaloc::1.0@" + host + ":" + port + "/IC");
    if ((obj == null) || (obj._non_existent())) {
      throw new ACSUnavailableException();
    }
    return IComponentHelper.narrow(obj);
  }

  /**
   * Gera a resposta para o desafio gerado pelo servi�o de controle de acesso.
   * 
   * @param challenge O desafio.
   * @param privateKey A chave privada de quem est� respondendo ao desafio.
   * @param acsCertificate O certificado do servi�o de controle de acesso
   * 
   * @return A resposta para o desafio.
   * 
   * @throws GeneralSecurityException Caso ocorra algum problema durante a
   *         opera��o.
   */
  public static byte[] generateAnswer(byte[] challenge, PrivateKey privateKey,
    Certificate acsCertificate) throws GeneralSecurityException {
    byte[] plainChallenge = CryptoUtils.decrypt(privateKey, challenge);
    return CryptoUtils.encrypt(acsCertificate, plainChallenge);
  }

  /**
   * Obt�m um servi�o de dados.
   * 
   * @param registryService O servi�o de registro.
   * @param dataServiceId O identificador do Servi�o de Dados.
   * 
   * @return O servi�o de dados, ou {@code null}, caso n�o seja encontrado.
   * 
   * @throws CORBAException Caso ocorra alguma exce��o na infra-estrutura CORBA.
   */
  public static IHDataService getDataService(IRegistryService registryService,
    ComponentId dataServiceId) throws CORBAException {
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
