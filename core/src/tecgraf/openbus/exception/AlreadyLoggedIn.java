package tecgraf.openbus.exception;


public final class AlreadyLoggedIn extends OpenBusException {
  public AlreadyLoggedIn() {
    super("Já existe um login ativo nesta conexão");
  }
}
