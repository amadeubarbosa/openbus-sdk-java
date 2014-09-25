package tecgraf.openbus.core;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;

import tecgraf.openbus.PrivateKey;
import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.security.Cryptography;

/**
 * Representa uma chave privada RSA que é compatível com o protocolo do OpenBus
 * 
 * @author Tecgraf
 */
public class OpenBusPrivateKey implements PrivateKey {

  /** A chave privada */
  private RSAPrivateKey key;

  /**
   * Construtor.
   * 
   * @param key chave privada RSA.
   */
  private OpenBusPrivateKey(RSAPrivateKey key) {
    this.key = key;
  }

  /**
   * Cria uma chave privada compatível com o protocolo do OpenBus à partir de
   * uma representação de chave RSA em formato de array de bytes.
   * 
   * @param privateKeyBytes a chave em formato de array de bytes.
   * @return a chave privada no formato reconhecível pela API do OpenBus.
   * @throws InvalidKeySpecException
   * @throws CryptographyException
   */
  static public OpenBusPrivateKey createPrivateKeyFromBytes(
    byte[] privateKeyBytes) throws InvalidKeySpecException,
    CryptographyException {
    RSAPrivateKey privateKey =
      Cryptography.getInstance().readPrivateKeyFromBytes(privateKeyBytes);
    return new OpenBusPrivateKey(privateKey);
  }

  /**
   * Cria uma chave privada compatível com o protocolo do OpenBus à partir de
   * uma representação de chave RSA em formato de arquivo.
   * 
   * @param privateKeyFile o caminho da chave em formato de arquivo
   * @return a chave privada no formato reconhecível pela API do OpenBus.
   * @throws IOException
   * @throws CryptographyException
   * @throws InvalidKeySpecException
   */
  static public OpenBusPrivateKey createPrivateKeyFromFile(String privateKeyFile)
    throws IOException, CryptographyException, InvalidKeySpecException {
    Cryptography crypto = Cryptography.getInstance();
    byte[] privKeyBytes = crypto.readKeyFromFile(privateKeyFile);
    return createPrivateKeyFromBytes(privKeyBytes);
  }

  /**
   * Recupera a chave RSA.
   * 
   * @return a chave RSA.
   */
  RSAPrivateKey getRSAPrivateKey() {
    return this.key;
  }
}
