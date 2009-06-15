package tecgraf.openbus.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

public final class PrivateKeyValidator {
  public static void main(String[] args) throws GeneralSecurityException,
    IOException {
    if (args.length == 0) {
      System.out.print("O arquivo de chave privada não foi especificado.");
      System.out.println(" Use: PrivateKeyValidator <file>");
      System.exit(0);
    }
    String privateKeyFileName = args[0];
    System.out.println("Arquivo a ser verificado: " + privateKeyFileName);
    RSAPrivateKey key = CryptoUtils.readPrivateKey(privateKeyFileName);
    System.out.println("Chave carregada com sucesso.");
    System.out.println("Formato: ");
    System.out.println(key.getFormat());
    System.out.println("Algoritmo: ");
    System.out.println(key.getAlgorithm());
  }
}
