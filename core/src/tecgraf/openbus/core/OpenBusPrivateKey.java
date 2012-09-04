package tecgraf.openbus.core;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;

import tecgraf.openbus.PrivateKey;
import tecgraf.openbus.util.Cryptography;

public class OpenBusPrivateKey implements PrivateKey {

  private RSAPrivateKey key;

  private OpenBusPrivateKey(RSAPrivateKey key) {
    this.key = key;
  }

  static public OpenBusPrivateKey createPrivateKeyFromBytes(
    byte[] privateKeyBytes) throws NoSuchAlgorithmException,
    InvalidKeySpecException {
    RSAPrivateKey privateKey =
      Cryptography.getInstance().readKeyFromBytes(privateKeyBytes);
    return new OpenBusPrivateKey(privateKey);
  }

  static public OpenBusPrivateKey createPrivateKeyFromFile(String privateKeyFile)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    Cryptography crypto = Cryptography.getInstance();
    byte[] privKeyBytes = crypto.readKeyFromFile(privateKeyFile);
    return createPrivateKeyFromBytes(privKeyBytes);
  }

  RSAPrivateKey getRSAPrivateKey() {
    return this.key;
  }
}
