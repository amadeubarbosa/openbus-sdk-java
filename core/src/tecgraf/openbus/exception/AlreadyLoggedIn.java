package tecgraf.openbus.exception;


public final class AlreadyLoggedIn extends OpenBusException {
  public AlreadyLoggedIn() {
    super("J� existe um login ativo nesta conex�o");
  }
}
