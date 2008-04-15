/*
 * $Id$
 */
package openbus.common;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import openbusidl.acs.IAccessControlService;
import openbusidl.acs.IAccessControlServiceHelper;
import openbusidl.rs.IRegistryService;
import openbusidl.rs.ServiceOffer;
import openbusidl.ss.ISessionService;
import openbusidl.ss.ISessionServiceHelper;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.IComponent;

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
   * Representa a interface do servi�o de sess�o.
   */
  private static final String SESSION_SERVICE_INTERFACE =
    "IDL:openbusidl/ss/ISessionService:1.0";

  /**
   * O tipo do servi�o de sess�o.
   */
  private static final String SESSION_SERVICE_TYPE = "SessionService";

  /**
   * Obt�m o servi�o de controle de acesso.
   * 
   * @param orb O orb utilizado para obter o servi�o.
   * @param host A m�quina onde o servi�o est� localizado.
   * @param port A porta onde o servi�o est� dispon�vel.
   * 
   * @return O servi�o de controle de acesso, ou {@code null}, caso n�o seja
   *         encontrado.
   */
  public static IAccessControlService fetchAccessControlService(ORB orb,
    String host, int port) {
    String url = "corbaloc::1.0@" + host + ":" + port + "/ACS";
    org.omg.CORBA.Object obj = orb.string_to_object(url);
    try {
      if (obj._non_existent()) {
        return null;
      }
    }
    catch (TRANSIENT e) {
      Log.COMMON.severe("Falha no acesso ao servi�o de controle de acesso.", e);
      return null;
    }
    return IAccessControlServiceHelper.narrow(obj);
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
   * Obt�m o RootPOA.
   * 
   * <p>
   * OBS: O POAManager � ativado neste m�todo.
   * 
   * @param orb O ORB para obten��o do RootPOA.
   * 
   * @return O RootPOA.
   * 
   * @throws InvalidName
   * @throws AdapterInactive
   */
  public static POA getRootPoa(ORB orb) throws InvalidName, AdapterInactive {
    Object rootPoaObject = orb.resolve_initial_references("RootPOA");
    POA rootPoa = POAHelper.narrow(rootPoaObject);
    rootPoa.the_POAManager().activate();
    return rootPoa;
  }

  /**
   * Obt�m o servi�o de sess�o.
   * 
   * @param registryService O servi�o de registro.
   * 
   * @return O servi�o de sess�o, ou {@code null}, caso n�o seja encontrado.
   */
  public static ISessionService getSessionService(
    IRegistryService registryService) {
    if (registryService == null) {
      throw new IllegalArgumentException(
        "O servi�o de registro n�o pode ser nulo.");
    }
    ServiceOffer[] offers =
      registryService.find(SESSION_SERVICE_TYPE, new openbusidl.rs.Property[0]);
    if (offers.length <= 0) {
      return null;
    }
    IComponent component = offers[0].member;
    Object facet = component.getFacet(SESSION_SERVICE_INTERFACE);
    return ISessionServiceHelper.narrow(facet);
  }
}
