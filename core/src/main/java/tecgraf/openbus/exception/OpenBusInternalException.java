package tecgraf.openbus.exception;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exceção que representa uma condição imprevista na biblioteca de acesso,
 * geralmente sem um tratamento óbvio.
 * 
 * @author Tecgraf
 */
public final class OpenBusInternalException extends IllegalStateException {
  /**
   * Construtor.
   * 
   * @param message Mensagem.
   * @param cause Causa.
   */
  public OpenBusInternalException(String message, Throwable cause) {
    super(message, cause);
    Logger logger = Logger.getLogger(OpenBusInternalException.class.getName());
    logger.log(Level.SEVERE, message, cause);
  }

  /**
   * Construtor.
   * 
   * @param message Mensagem.
   */
  public OpenBusInternalException(String message) {
    super(message);
    Logger logger = Logger.getLogger(OpenBusInternalException.class.getName());
    logger.log(Level.SEVERE, message);
  }
}
