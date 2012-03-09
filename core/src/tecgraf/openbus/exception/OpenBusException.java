package tecgraf.openbus.exception;

public abstract class OpenBusException extends Exception {
  protected OpenBusException(String message) {
    super(message);
  }

  protected OpenBusException(Throwable cause) {
    super(cause);
  }

  protected OpenBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
