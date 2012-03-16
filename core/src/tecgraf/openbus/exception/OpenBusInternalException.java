package tecgraf.openbus.exception;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exceção interna do barramento.
 * 
 * @author Tecgraf
 */
public final class OpenBusInternalException extends IllegalStateException {

  /**
   * Construtor.
   * 
   * @param message mensagem.
   * @param cause causa.
   */
  public OpenBusInternalException(String message, Throwable cause) {
    super(message, cause);
    Logger logger = Logger.getLogger(OpenBusInternalException.class.getName());
    logger.log(Level.SEVERE, message, cause);
  }

  /**
   * Construtor.
   * 
   * @param message mensagem
   */
  public OpenBusInternalException(String message) {
    super(message);
    Logger logger = Logger.getLogger(OpenBusInternalException.class.getName());
    logger.log(Level.SEVERE, message);
  }
}
