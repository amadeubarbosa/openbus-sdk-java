package tecgraf.openbus.exception;

public class WrongPrivateKey extends OpenBusException {

  protected WrongPrivateKey(String message) {
    super(message);
  }

  public WrongPrivateKey(String message, Throwable exception) {
    super(message, exception);
  }

}
