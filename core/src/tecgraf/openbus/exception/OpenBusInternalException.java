package tecgraf.openbus.exception;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class OpenBusInternalException extends IllegalStateException {

  public OpenBusInternalException(String message, Throwable cause) {
    super(message, cause);
    Logger logger = Logger.getLogger(OpenBusInternalException.class.getName());
    logger.log(Level.SEVERE, message, cause);
  }
}
