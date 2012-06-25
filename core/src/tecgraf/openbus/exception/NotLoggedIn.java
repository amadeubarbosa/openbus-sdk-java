package tecgraf.openbus.exception;

public final class NotLoggedIn extends OpenBusException {

  //TODO verificar quais construtores
  public NotLoggedIn() {
    super("Conexão não está autenticada.");
  }

}
