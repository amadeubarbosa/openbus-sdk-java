package tecgraf.openbus.exception;


public final class AlreadyLoggedException extends OpenBusException {
  public AlreadyLoggedException() {
    super("J� existe um login ativo nesta conex�o");
  }
}
