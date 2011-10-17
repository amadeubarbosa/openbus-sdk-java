package tecgraf.openbus;

import java.security.GeneralSecurityException;

public final class CryptographyException extends OpenBusException {
  public CryptographyException(String message) {
    super(message);
  }

  public CryptographyException(GeneralSecurityException cause) {
    super(cause);
  }
}
