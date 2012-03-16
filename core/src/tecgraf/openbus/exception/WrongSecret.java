package tecgraf.openbus.exception;

public class WrongSecret extends OpenBusException {

  protected WrongSecret(String message) {
    super(message);
  }

  public WrongSecret(String message, Throwable exception) {
    super(message, exception);
  }

}
