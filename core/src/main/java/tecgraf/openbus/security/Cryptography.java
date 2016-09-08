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
 * Classe utilitária de criptografia.
 * 
 * @author Tecgraf
 */
public final class Cryptography {
  /**
   * Tipo de certificado
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
   * Tamanho da chave de criptografia.
   * <p>
   * Qualquer dado criptografado resulta em um array de tamanho múltiplo do
   * tamanho da chave.
   */
  private static final int ENCRIPTION_KEY_SIZE = 256;
  /**
   * Tamanho máximo de dado que pode ser encriptado por bloco, devido a
   * necessidade de inclusão de padding
   */
  private static final int MAX_BLOCK_ENCRIPTION_SIZE = 245;

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
   * Fornece a única instância (singleton) desta classe.
   * 
   * @return A instância.
   */
  public static synchronized Cryptography getInstance() {
    if (instance == null) {
      instance = new Cryptography();
    }
    return instance;
  }

  /**
   * Criptografa um dado através de um certificado.
   * 
   * @param data o dado a ser criptografado.
   * @param certificate o certificado
   * @return O dado criptografado.
   * @throws CryptographyException Caso algum erro ocorra na operação.
   */
  public byte[] encrypt(byte[] data, X509Certificate certificate)
    throws CryptographyException {
    return this.encrypt(data, (RSAPublicKey) certificate.getPublicKey());
  }

  /**
   * Criptografa um dado através de uma chave pública.
   * 
   * @param data o dado a ser criptografado.
   * @param publicKey a chave pública.
   * @return O dado criptografado.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
   */
  public byte[] encrypt(byte[] data, RSAPublicKey publicKey)
    throws CryptographyException {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      synchronized (cipher) {
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        int blocks =
          (int) Math.ceil((double) data.length / MAX_BLOCK_ENCRIPTION_SIZE);
        int offset = 0;
        int lenght = MAX_BLOCK_ENCRIPTION_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(ENCRIPTION_KEY_SIZE * blocks);
        while (offset < data.length) {
          if (offset + lenght > data.length) {
            lenght = data.length - offset;
          }
          buffer.put(cipher.doFinal(data, offset, lenght));
          offset += lenght;
        }
        return buffer.array();
      }
    }
    catch (GeneralSecurityException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Decripta um dado através de uma chava privada.
   * 
   * @param data o dado criptografado.
   * @param privateKey a chave privada.
   * @return o dado descriptografado.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
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
   * Gera uma chave pública a partir de uma chave codificada em X.509.
   * 
   * @param encodedKey A chave codificada.
   * @return A chave pública RSA.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
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
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
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
   * Gera um hash do dado, utilizando o algoritmo SHA-256.
   * 
   * @param data O dado.
   * @return O hash do dado.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
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
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
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
   * Fornece a chave privada contida no arquivo especificado.
   * 
   * @param privateKeyFileName O caminho para o arquivo.
   * @return A chave privada RSA.
   * @throws IOException Caso algum erro ocorra ao acessar o arquivo da chave
   * privada.
   * @throws InvalidKeySpecException A chave privada é inválida.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
   */
  public RSAPrivateKey readKeyFromFile(String privateKeyFileName)
    throws IOException, InvalidKeySpecException, CryptographyException {
    try (FileInputStream fis = new FileInputStream(privateKeyFileName)) {
      FileChannel channel = fis.getChannel();
      ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
      int size = channel.read(buffer);
      if (size != (int) channel.size()) {
        throw new IOException("Não foi possível ler todo o arquivo.");
      }
      return readKeyFromBytes(buffer.array());
    }
  }

  /**
   * Interpreta uma chave privada a partir de um array de bytes.
   * 
   * @param privateKeyBytes Os bytes da chave privada.
   * @return A chave privada RSA.
   * @throws InvalidKeySpecException A chave privada é inválida.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
   */
  public RSAPrivateKey readKeyFromBytes(byte[] privateKeyBytes)
    throws InvalidKeySpecException, CryptographyException {
    PKCS8EncodedKeySpec encodedKey = new PKCS8EncodedKeySpec(privateKeyBytes);
    try {
      KeyFactory kf = KeyFactory.getInstance(KEY_FACTORY);
      synchronized (kf) {
        return (RSAPrivateKey) kf.generatePrivate(encodedKey);
      }
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Fornece um par de chaves a partir de um caminho para uma chave privada.
   * 
   * @param path Caminho para o arquivo da chave privada.
   * @return O par de chaves RSA.
   * @throws InvalidKeySpecException A chave privada é inválida.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
   * @throws IOException Caso algum erro ocorra ao acessar o arquivo das chaves.
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
        return new KeyPair(pubKey, privKey);
      }
    }
    catch (NoSuchAlgorithmException e) {
      throw new CryptographyException(e);
    }
  }

  /**
   * Verifica a assinatura de um dado.
   * 
   * @param publicKey A chave pública a ser utilizada na verificação.
   * @param rawData O dado supostamente assinado.
   * @param signedData O dado assinado.
   * @return {@code True} caso a assinatura seja válida, e {@code false}
   *         caso contrário.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
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
   * @return O certificado lido.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
   * @throws IOException Caso algum erro ocorra ao acessar o arquivo do
   * certificado.
   */
  public X509Certificate readX509Certificate(String certificateFile)
    throws CryptographyException, IOException {
    return readX509Certificate(new FileInputStream(certificateFile));
  }

  /**
   * Lê um certificado digital a partir de um fluxo de arquivo.
   * 
   * @param inputStream Fluxo do certificado.
   * 
   * @return O certificado lido.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
   * @throws IOException Caso algum erro ocorra ao acessar o arquivo de
   * certificado.
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
   * Interpreta um certificado digital a partir de um <i>array</i> de
   * <i>bytes</i>.
   * 
   * @param encoded O certificado em <i>bytes</i>.
   * 
   * @return O certificado interpretado.
   * @throws CryptographyException Caso algum erro ocorra ao utilizar os
   * algoritmos criptográficos.
   * @throws IOException Caso algum erro ocorra ao acessar os bytes como um
   * fluxo.
   */
  public X509Certificate readX509Certificate(byte[] encoded)
    throws CryptographyException, IOException {
    ByteArrayInputStream stream = new ByteArrayInputStream(encoded);
    return readX509Certificate(stream);
  }
}
