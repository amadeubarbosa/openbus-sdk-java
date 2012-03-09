package tecgraf.openbus.exception;


public final class AlreadyLoggedException extends OpenBusException {
  public AlreadyLoggedException() {
    super("Já existe um login ativo nesta conexão");
  }
}
