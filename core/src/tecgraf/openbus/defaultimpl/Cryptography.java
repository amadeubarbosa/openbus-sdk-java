package tecgraf.openbus.defaultimpl;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import tecgraf.openbus.CryptographyException;
import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChain;

final class Cryptography {
  public static final int HASH_VALUE_SIZE = 32;
  public static final byte[] NULL_HASH_VALUE = new byte[HASH_VALUE_SIZE];

  public static final int ENCRYPTED_BLOCK_SIZE = 256;
  public static final byte[] NULL_ENCRYPTED_BLOCK =
    new byte[ENCRYPTED_BLOCK_SIZE];

  public static final SignedCallChain NULL_SIGNED_CALL_CHAIN =
    new SignedCallChain(NULL_ENCRYPTED_BLOCK, new byte[0]);

  private static final String HASH_ALGORITHM = "SHA-256";

  private static final String KEY_FACTORY = "RSA";
  /**
   * O algoritmo de criptografia (assimétrica) utilizada pelo OpenBus.
   */
  public static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1PADDING";

  private static Cryptography instance;

  private Cryptography() {
  }

  public static Cryptography getInstance() {
    if (instance == null) {
      instance = new Cryptography();
    }
    return instance;
  }

  public byte[] encrypt(byte[] data, X509Certificate certificate)
    throws CryptographyException {
    return this.encrypt(data, (RSAPublicKey) certificate.getPublicKey());
  }

  public byte[] encrypt(byte[] data, RSAPublicKey publicKey)
    throws CryptographyException {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
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

  public RSAPublicKey generateRSAPublicKeyFromX509EncodedKey(byte[] encodedKey)
    throws CryptographyException {
    EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(encodedKey);
    try {
      KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY);
      return (RSAPublicKey) keyFactory.generatePublic(encodedKeySpec);
    }
    catch (GeneralSecurityException e) {
      throw new CryptographyException(e);
    }
  }

  public KeyPair generateRSAKeyPair() throws CryptographyException {
    KeyPairGenerator keyPairGenerator;
    try {
      keyPairGenerator = KeyPairGenerator.getInstance(KEY_FACTORY);
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
    keyPairGenerator.initialize(2048, new SecureRandom());
    return keyPairGenerator.genKeyPair();
  }

  public byte[] generateHash(byte[] data) throws CryptographyException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASH_ALGORITHM);
      return digest.digest(data);
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
  }

  public MessageDigest getHashAlgorithm() throws CryptographyException {
    try {
      return MessageDigest.getInstance(HASH_ALGORITHM);
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
  }
}
