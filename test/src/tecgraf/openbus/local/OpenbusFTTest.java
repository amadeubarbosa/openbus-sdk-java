package tecgraf.openbus.local;

import java.io.IOException;

import org.junit.Before;
import org.omg.CORBA.UserException;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.exception.OpenBusException;

public class OpenbusFTTest extends OpenbusTest {

  public OpenbusFTTest() throws IOException {
    super();
  }

  /**
   * Este método é chamado antes de cada testCase.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Override
  @Before
  public void beforeTest() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    openbus.initWithFaultTolerance(null, props, hostName, hostPort);
  }

}