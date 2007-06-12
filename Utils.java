/*
 * $Id$
 */
package openbus.common;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import openbusidl.acs.IAccessControlService;
import openbusidl.acs.IAccessControlServiceHelper;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.ORB;

/**
 * M�todos utilit�rios para uso do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Utils {
  /**
   * Representa a interface do servi�o de controle de acesso.
   */
  public static final String ACCESS_CONTROL_SERVICE_INTERFACE = "IDL:openbusidl/acs/IAccessControlService:1.0";

  /**
   * Representa a interface do servi�o de registro.
   */
  public static final String REGISTRY_SERVICE_INTERFACE = "IDL:openbusidl/rs/IRegistryService:1.0";

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
    catch (COMM_FAILURE e) {
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
}

