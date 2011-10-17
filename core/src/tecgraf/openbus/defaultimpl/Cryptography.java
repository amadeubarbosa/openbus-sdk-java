package tecgraf.openbus.defaultimpl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

import javax.crypto.Cipher;

import tecgraf.openbus.CryptographyException;

final class Cryptography {
  /**
   * O algoritmo de criptografia (assimétrica) utilizada pelo OpenBus.
   */
  private static final String CIPHER_ALGORITHM = "RSA";

  private static Cryptography instance;

  private Cryptography() {
  }

  public static Cryptography getInstance() {
    if (instance == null) {
      instance = new Cryptography();
    }
    return instance;
  }

  public byte[] encrypt(char[] data, X509Certificate certificate)
    throws CryptographyException {
    Charset charset = Charset.forName("ISO-8859-1");
    CharBuffer dataBuffer = CharBuffer.wrap(data);
    ByteBuffer dataByteBuffer = charset.encode(dataBuffer);
    return this.encrypt(dataByteBuffer.array(), certificate);
  }

  public byte[] encrypt(byte[] data, X509Certificate certificate)
    throws CryptographyException {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, certificate);
      return cipher.doFinal(data);
    }
    catch (GeneralSecurityException e) {
      throw new CryptographyException(e);
    }
  }

  public byte[] decrypt(byte[] data, RSAPrivateKey privateKey)
    throws CryptographyException {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
      return cipher.doFinal(data);
    }
    catch (GeneralSecurityException e) {
      throw new CryptographyException(e);
    }
  }
}
