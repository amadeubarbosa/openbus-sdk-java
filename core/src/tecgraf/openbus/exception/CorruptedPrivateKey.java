package tecgraf.openbus.exception;

public final class CorruptedPrivateKey extends OpenBusException {
  public CorruptedPrivateKey(String message) {
    super(message);
  }

  public CorruptedPrivateKey(String message, Throwable exception) {
    super(message, exception);
  }
}
