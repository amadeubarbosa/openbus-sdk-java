/*
 * $Id$
 */
package tecgraf.openbus.util;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TRANSIENT;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;

/**
 * M�todos utilit�rios para uso do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Utils {
  /**
   * Representam a chave para obten��o do barramento.
   */
  public static final String OPENBUS_KEY = "openbus_v1_05";

  /**
   * Nome da faceta IReceptacles.
   */
  public static final String RECEPTACLES_NAME = "IReceptacles";
  /**
   * Nome do recept�culo do Servi�o de Registro.
   */
  public static final String REGISTRY_SERVICE_RECEPTACLE_NAME =
    "RegistryServiceReceptacle";

  /**
   * O nome da faceta do Servi�o de Registro.
   */
  public static final String REGISTRY_SERVICE_FACET_NAME = "IRegistryService";
  /**
   * O nome da faceta do Servi�o de Sess�o.
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
   * Obt�m o componente do Servi�o de Controle de Acesso.
   * 
   * @param orb O orb utilizado para obter o servi�o.
   * @param host A m�quina onde o servi�o est� localizado.
   * @param port A porta onde o servi�o est� dispon�vel.
   * 
   * @return O componente do Servi�o de Controle de Acesso.
   * 
   * @throws ACSUnavailableException Caso o servi�o n�o seja encontrado.
   * @throws CORBAException Caso ocorra algum erro no acesso ao servi�o.
   */
  public static IComponent fetchAccessControlServiceComponent(ORB orb,
    String host, int port) throws ACSUnavailableException, CORBAException {
    org.omg.CORBA.Object obj =
      orb.string_to_object("corbaloc::1.0@" + host + ":" + port + "/"
        + Utils.OPENBUS_KEY);
    if (obj == null) {
      throw new ACSUnavailableException();
    }
    try {
      if (obj._non_existent()) {
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
      throw new CORBAException(e);
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
}
