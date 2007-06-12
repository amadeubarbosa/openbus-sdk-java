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
 * Métodos utilitários para uso do OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class Utils {
  /**
   * Representa a interface do serviço de controle de acesso.
   */
  public static final String ACCESS_CONTROL_SERVICE_INTERFACE = "IDL:openbusidl/acs/IAccessControlService:1.0";

  /**
   * Representa a interface do serviço de registro.
   */
  public static final String REGISTRY_SERVICE_INTERFACE = "IDL:openbusidl/rs/IRegistryService:1.0";

  /**
   * Obtém o serviço de controle de acesso.
   * 
   * @param orb O orb utilizado para obter o serviço.
   * @param host A máquina onde o serviço está localizado.
   * @param port A porta onde o serviço está disponível.
   * 
   * @return O serviço de controle de acesso, ou {@code null}, caso não seja
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

