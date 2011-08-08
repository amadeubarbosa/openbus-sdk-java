package tecgraf.openbus.exception;

/**
 * Indica que o Openbus j� foi inicializado.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class AlreadyInitializedException extends OpenBusException {
  public AlreadyInitializedException(String message) {
    super(message);
  }
}
