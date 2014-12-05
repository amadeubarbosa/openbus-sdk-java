package tecgraf.openbus.security;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import tecgraf.openbus.exception.CryptographyException;

/**
 * Classe de utilitária de criptografia.
 * 
 * @author Tecgraf
 */
public final class Cryptography {
  /**
   * Tipo de certficado
   */
  private static final String CERT_FACTORY = "X.509";
  /**
   * Algoritmo do hash
   */
  private static final String HASH_ALGORITHM = "SHA-256";
  /**
   * Tipo da chave
   */
  private static final String KEY_FACTORY = "RSA";
  /**
   * O algoritmo de criptografia (assimétrica) utilizada pelo OpenBus.
   */
  public static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1PADDING";
  /**
   * Codificação charset padrão.
   */
  public static final Charset CHARSET = Charset.forName("US-ASCII");
  /**
   * A instância.
   */
  private static Cryptography instance;

  /**
   * Construtor.
   */
  private Cryptography() {
  }

  /**
   * Recupera a instância (singleton) desta classe.
   * 
   * @return a instância
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
   * Criptografa o dado com a chave pública.
   * 
   * @param data o dado a ser criptografado.
   * @param publicKey a chave pública.
   * @return O dado criptografado.
   * @throws CryptographyException
   */
  public byte[] encrypt(byte[] data, RSAPublicKey publicKey)
    throws CryptographyException {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      synchronized (cipher) {
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
      }
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
      synchronized (cipher) {
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
      }
    }
    catch (GeneralSecurityException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Gera uma chave pública à partir da chave codificada em X.509
   * 
   * @param encodedKey chave codificada.
   * @return A chave pública RSA
   * @throws CryptographyException
   */
  public RSAPublicKey generateRSAPublicKeyFromX509EncodedKey(byte[] encodedKey)
    throws CryptographyException {
    EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(encodedKey);
    try {
      KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY);
      synchronized (keyFactory) {
        return (RSAPublicKey) keyFactory.generatePublic(encodedKeySpec);
      }
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
      synchronized (keyPairGenerator) {
        keyPairGenerator.initialize(2048, new SecureRandom());
        return keyPairGenerator.genKeyPair();
      }
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }

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
      synchronized (digest) {
        return digest.digest(data);
      }
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Obtém o algortimo de Hash.
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
   * Recupera a chave privada contida no arquivo fornecido.
   * 
   * @param privateKeyFileName o path para o arquivo.
   * @return A chave privada RSA.
   * @throws IOException
   * @throws InvalidKeySpecException
   * @throws CryptographyException
   */
  public RSAPrivateKey readKeyFromFile(String privateKeyFileName)
    throws IOException, InvalidKeySpecException, CryptographyException {
    FileInputStream fis = new FileInputStream(privateKeyFileName);
    try {
      FileChannel channel = fis.getChannel();
      ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
      int size = channel.read(buffer);
      if (size != (int) channel.size()) {
        throw new IOException("Não foi possível ler todo o arquivo.");
      }
      return readKeyFromBytes(buffer.array());
    }
    finally {
      fis.close();
    }
  }

  /**
   * Recupera a chave privada a partir de um array de bytes.
   * 
   * @param privateKeyBytes bytes da chave privada
   * @return A chave privada RSA.
   * @throws InvalidKeySpecException
   * @throws CryptographyException
   */
  public RSAPrivateKey readKeyFromBytes(byte[] privateKeyBytes)
    throws InvalidKeySpecException, CryptographyException {
    PKCS8EncodedKeySpec encodedKey = new PKCS8EncodedKeySpec(privateKeyBytes);
    try {
      KeyFactory kf = KeyFactory.getInstance(KEY_FACTORY);
      synchronized (kf) {
        RSAPrivateKey privKey = (RSAPrivateKey) kf.generatePrivate(encodedKey);
        return privKey;
      }
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Recupera o par de chaves a partir de um array do path para uma chave
   * privada.
   * 
   * @param path bytes da chave privada
   * @return A chave privada RSA.
   * @throws InvalidKeySpecException
   * @throws CryptographyException
   * @throws IOException
   */
  public KeyPair readKeyPairFromFile(String path)
    throws InvalidKeySpecException, CryptographyException, IOException {
    try {
      RSAPrivateCrtKey privKey = (RSAPrivateCrtKey) readKeyFromFile(path);
      KeyFactory kf = KeyFactory.getInstance(KEY_FACTORY);
      synchronized (kf) {
        RSAPublicKey pubKey =
          (RSAPublicKey) kf.generatePublic(new RSAPublicKeySpec(privKey
            .getModulus(), privKey.getPublicExponent()));
        KeyPair keyPair = new KeyPair(pubKey, privKey);
        return keyPair;
      }
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Verifica a assinatura de um dado.
   * 
   * @param publicKey a chave publica a ser utilizada na verificação.
   * @param rawData o dado supostamente assinado.
   * @param signedData o dado assinado.
   * @return <code>true</code> caso a assinatura é válida, e <code>false</code>
   *         caso contrário.
   * @throws CryptographyException
   */
  public boolean verifySignature(RSAPublicKey publicKey, byte[] rawData,
    byte[] signedData) throws CryptographyException {
    try {
      Signature sign = Signature.getInstance("NONEwithRSA");
      synchronized (sign) {
        sign.initVerify(publicKey);
        byte[] hashData = this.generateHash(rawData);
        sign.update(hashData);
        return sign.verify(signedData);
      }
    }
    catch (GeneralSecurityException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Lê um certificado digital a partir de um arquivo.
   * 
   * @param certificateFile O caminho para o arquivo de certificado.
   * 
   * @return O certificado carregado.
   * @throws CryptographyException
   * @throws IOException
   */
  public X509Certificate readX509Certificate(String certificateFile)
    throws CryptographyException, IOException {
    return readX509Certificate(new FileInputStream(certificateFile));
  }

  /**
   * Lê um certificado digital a partir de um stream de arquivo.
   * 
   * @param inputStream arquivo de certificado.
   * 
   * @return O certificado carregado.
   * @throws CryptographyException
   * @throws IOException
   */
  public X509Certificate readX509Certificate(InputStream inputStream)
    throws CryptographyException, IOException {
    try {
      CertificateFactory cf = CertificateFactory.getInstance(CERT_FACTORY);
      return (X509Certificate) cf.generateCertificate(inputStream);
    }
    catch (CertificateException e) {
      throw new CryptographyException(e);
    }
    finally {
      inputStream.close();
    }
  }

  /**
   * Lê um certificado digital a partir de um stream de arquivo.
   * 
   * @param encoded arquivo de certificado.
   * 
   * @return O certificado carregado.
   * @throws CryptographyException
   * @throws IOException
   */
  public X509Certificate readX509Certificate(byte[] encoded)
    throws CryptographyException, IOException {
    ByteArrayInputStream stream = new ByteArrayInputStream(encoded);
    return readX509Certificate(stream);
  }
}
