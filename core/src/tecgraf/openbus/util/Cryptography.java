package tecgraf.openbus.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChain;
import tecgraf.openbus.exception.CryptographyException;

/**
 * Classe de utilit�ria de criptografia.
 * 
 * @author Tecgraf
 */
public final class Cryptography {
  /**
   * Tamanho do hash
   */
  public static final int HASH_VALUE_SIZE = 32;
  /**
   * Hash nulo.
   */
  public static final byte[] NULL_HASH_VALUE = new byte[HASH_VALUE_SIZE];
  /**
   * Tamanho do bloco criptografado
   */
  public static final int ENCRYPTED_BLOCK_SIZE = 256;
  /**
   * Bloco nulo criptografado.
   */
  public static final byte[] NULL_ENCRYPTED_BLOCK =
    new byte[ENCRYPTED_BLOCK_SIZE];
  /**
   * Cadeia nula assinada.
   */
  public static final SignedCallChain NULL_SIGNED_CALL_CHAIN =
    new SignedCallChain(NULL_ENCRYPTED_BLOCK, new byte[0]);
  /**
   * Algoritmo do hash
   */
  private static final String HASH_ALGORITHM = "SHA-256";
  /**
   * Tipo da chave
   */
  private static final String KEY_FACTORY = "RSA";
  /**
   * O algoritmo de criptografia (assim�trica) utilizada pelo OpenBus.
   */
  public static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1PADDING";
  /**
   * Codifica��o charset padr�o.
   */
  public static final Charset CHARSET = Charset.forName("US-ASCII");
  /**
   * A inst�ncia.
   */
  private static Cryptography instance;

  /**
   * Construtor.
   */
  private Cryptography() {
  }

  /**
   * Recupera a inst�ncia (singleton) desta classe.
   * 
   * @return a inst�ncia
   */
  public static synchronized Cryptography getInstance() {
    if (instance == null) {
      instance = new Cryptography();
    }
    return instance;
  }

  /**
   * Criptografa o dado com o certificado.
   * 
   * @param data o dado a ser criptografado.
   * @param certificate o certificado
   * @return O dado criptografado.
   * @throws CryptographyException
   */
  public byte[] encrypt(byte[] data, X509Certificate certificate)
    throws CryptographyException {
    return this.encrypt(data, (RSAPublicKey) certificate.getPublicKey());
  }

  /**
   * Criptografa o dado com a chave p�blica.
   * 
   * @param data o dado a ser criptografado.
   * @param publicKey a chave p�blica.
   * @return O dado criptografado.
   * @throws CryptographyException
   */
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

  /**
   * Descriptografa o dado com a chava privada.
   * 
   * @param data o dado criptografado.
   * @param privateKey a chave privada.
   * @return o dado descriptografado.
   * @throws CryptographyException
   */
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

  /**
   * Gera uma chave p�blica � partir da chave codificada em X.509
   * 
   * @param encodedKey chave codificada.
   * @return A chave p�blica RSA
   * @throws CryptographyException
   */
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

  /**
   * Gera um par de chaves RSA.
   * 
   * @return o par de chaves.
   * @throws CryptographyException
   */
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

  /**
   * Gera um hash do dado, utilizando o algoritmo SHA-256
   * 
   * @param data o dado
   * @return o hash do dado.
   * @throws CryptographyException
   */
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

  /**
   * Obt�m o algortimo de Hash.
   * 
   * @return o algoritmo.
   * @throws CryptographyException
   */
  public MessageDigest getHashAlgorithm() throws CryptographyException {
    try {
      return MessageDigest.getInstance(HASH_ALGORITHM);
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Recupera a chave privada de um arquivo.
   * 
   * @param privateKeyFileName o path para o arquivo.
   * @return A chave privada RSA.
   * @throws IOException
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   */
  public RSAPrivateKey readPrivateKey(String privateKeyFileName)
    throws IOException, InvalidKeyException, NoSuchAlgorithmException,
    InvalidKeySpecException {
    FileInputStream fis = new FileInputStream(privateKeyFileName);
    FileChannel channel = fis.getChannel();
    ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
    int size = channel.read(buffer);
    if (size != (int) channel.size()) {
      throw new IOException("N�o foi poss�vel ler todo o arquivo.");
    }
    PKCS8EncodedKeySpec encodedKey = new PKCS8EncodedKeySpec(buffer.array());
    KeyFactory kf = KeyFactory.getInstance(KEY_FACTORY);
    return (RSAPrivateKey) kf.generatePrivate(encodedKey);
  }

  /**
   * Verifica a assinatura de um dado.
   * 
   * @param publicKey a chave publica a ser utilizada na verifica��o.
   * @param rawData o dado supostamente assinado.
   * @param signedData o dado assinado.
   * @return <code>true</code> caso a assinatura � v�lida, e <code>false</code>
   *         caso contr�rio.
   * @throws CryptographyException
   */
  public boolean verifySignature(RSAPublicKey publicKey, byte[] rawData,
    byte[] signedData) throws CryptographyException {
    try {
      Signature sign = Signature.getInstance("NONEwithRSA");
      sign.initVerify(publicKey);
      byte[] hashData = this.generateHash(rawData);
      sign.update(hashData);
      return sign.verify(signedData);
    }
    catch (GeneralSecurityException e) {
      throw new CryptographyException(e);
    }
  }
}