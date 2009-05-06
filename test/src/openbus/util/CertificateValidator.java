package openbus.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import openbus.common.CryptoUtils;

public final class CertificateValidator {
  public static void main(String[] args) throws GeneralSecurityException,
    IOException {
    if (args.length == 0) {
      System.out.print("O arquivo de certificado não foi especificado.");
      System.out.println(" Use: CertificateValidator <file>");
      System.exit(0);
    }
    String certificateFile = args[0];
    System.out.println("Arquivo a ser verificado: " + certificateFile);
    X509Certificate certificate = CryptoUtils.readCertificate(certificateFile);
    System.out.println("Certificado carregado com sucesso.");
    System.out.print("Tipo: ");
    System.out.println(certificate.getType());
  }
}
