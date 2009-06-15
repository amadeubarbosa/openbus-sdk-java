/*
 * $Id$
 */
package tecgraf.openbus.common;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;

/**
 * Métodos utilitários para uso de criptografia.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class CryptoUtils {
  /**
   * O tipo de fábrica de chaves privadas utilizado.
   */
  private static final String KEY_FACTORY_TYPE = "RSA";
  /**
   * O tipo de certificado utilizado pelo OpenBus.
   */
  private static final String CERTIFICATE_TYPE = "X.509";
  /**
   * O algoritmo de criptografia (assimétrica) utilizada pelo OpenBus.
   */
  private static final String CIPHER_ALGORITHM = "RSA";

  /**
   * Lê um certificado digital a partir de um arquivo.
   * 
   * @param certificateFile O nome do arquivo.
   * 
   * @return O certificado carregado.
   * 
   * @throws CertificateException Caso o arquivo esteja corrompido.
   * @throws FileNotFoundException Caso o arquivo não exista.
   */
  public static X509Certificate readCertificate(String certificateFile)
    throws CertificateException, FileNotFoundException {
    InputStream inputStream = new FileInputStream(certificateFile);
    try {
      CertificateFactory cf = CertificateFactory.getInstance(CERTIFICATE_TYPE);
      return (X509Certificate) cf.generateCertificate(inputStream);
    }
    finally {
      try {
        inputStream.close();
      }
      catch (IOException e) {
        Log.COMMON.warning(e.getLocalizedMessage());
      }
    }
  }

  /**
   * Lê uma chave privada a partir de um arquivo.
   * 
   * @param privateKeyFileName O nome do arquivo.
   * 
   * @return A chave privada carregada.
   * 
   * @throws NoSuchAlgorithmException Caso o algoritmo para criação da chave não
   *         seja encontrado.
   * @throws InvalidKeySpecException Caso o formato da chave seja inválido.
   * @throws InvalidKeyException Caso o formato da chave seja inválido.
   * @throws IOException Caso ocorra algum erro durante a leitura.
   * @throws FileNotFoundException Caso o arquivo não exista.
   */
  public static RSAPrivateKey readPrivateKey(String privateKeyFileName)
    throws NoSuchAlgorithmException, InvalidKeySpecException,
    InvalidKeyException, IOException, FileNotFoundException {
    byte[] encodedBuffer = readBytes(privateKeyFileName);
    Base64 base64 = new Base64();
    byte[] bytes = base64.decode(encodedBuffer);
    PKCS8EncodedKeySpec encodedKey = new PKCS8EncodedKeySpec(bytes);
    KeyFactory kf = KeyFactory.getInstance(KEY_FACTORY_TYPE);
    return (RSAPrivateKey) kf.generatePrivate(encodedKey);
  }

  /**
   * 
   * Lê os bytes de um arquivo que representa uma chave privada, no formato
   * PKCS#8 e na base 64, retirando o seu cabeçalho e o seu rodapé.
   * 
   * @param privateKeyFileName O nome (caminho completo) do arquivo contendo a
   *        chave privada.
   * 
   * @return Os bytes representando a chave privada na base 64.
   * 
   * @throws IOException Caso ocorra algum erro durante a leitura.
   * @throws InvalidKeyException Caso o formato da chave seja inválido.
   * @throws FileNotFoundException Caso o arquivo não exista.
   */
  private static byte[] readBytes(String privateKeyFileName)
    throws InvalidKeyException, IOException, FileNotFoundException {
    BufferedReader reader = new BufferedReader(new FileReader(
      privateKeyFileName));
    StringBuilder data = new StringBuilder();
    try {
      String line = reader.readLine();
      if (line == null || !line.equals("-----BEGIN PRIVATE KEY-----")) {
        throw new InvalidKeyException(
          "Formato do arquivo inválido: cabeçalho não encontrado.");
      }
      for (line = reader.readLine(); line != null; line = reader.readLine()) {
        if (line.equals("-----END PRIVATE KEY-----")) {
          return data.toString().getBytes();
        }
        data.append(line);
      }
      throw new InvalidKeyException(
        "Formato do arquivo inválido: rodapé não encontrado.");
    }
    finally {
      try {
        reader.close();
      }
      catch (IOException e) {
        // Nada a ser feito.
      }
    }
  }

  /**
   * Criptografa dados.
   * 
   * @param certificate O certificado digital de onde será extraído a chave
   *        pública para criptografar os dados.
   * 
   * @param data Os dados.
   * 
   * @return O texto criptografado.
   * 
   * @throws GeneralSecurityException Caso ocorra alguma falha com o
   *         procedimento.
   */
  public static byte[] encrypt(Certificate certificate, byte[] data)
    throws GeneralSecurityException {
    if (certificate == null) {
      throw new IllegalArgumentException("certificate == null");
    }
    if (data == null) {
      throw new IllegalArgumentException("data == null");
    }
    Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, certificate);
    return cipher.doFinal(data);
  }

  /**
   * Gera o texto plano a partir de um texto previamente criptografado.
   * 
   * @param privateKey A chave privada utilizada para gerar o texto plano (deve
   *        ser a chave correspondente à chave pública utilizada para gerar o
   *        texto criptografado).
   * @param encryptedData O texto criptografado.
   * 
   * @return O texto plano.
   * 
   * @throws GeneralSecurityException Caso ocorra alguma falha com o
   *         procedimento.
   */
  public static byte[] decrypt(PrivateKey privateKey, byte[] encryptedData)
    throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    return cipher.doFinal(encryptedData);
  }
}
