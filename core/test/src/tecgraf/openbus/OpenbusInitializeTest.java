package tecgraf.openbus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.UserException;

import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.exception.OpenbusAlreadyInitializedException;

public class OpenbusInitializeTest {

  private static Properties props;

  private String hostName;
  private int hostPort;
  private String userLogin;
  private String userPassword;

  /**
   * Construtor
   * 
   * @throws IOException
   */
  public OpenbusInitializeTest() throws IOException {
    // Carregando o arquivo de configuração
    Properties defaultProps = new Properties();
    InputStream in =
      this.getClass().getResourceAsStream("/AllTests.properties");
    defaultProps.load(in);
    in.close();

    this.userLogin = defaultProps.getProperty("User.Login");
    this.userPassword = defaultProps.getProperty("User.Password");
    this.hostName = defaultProps.getProperty("Host.Name");
    this.hostPort = Integer.valueOf(defaultProps.getProperty("Host.Port"));
  }

  /**
   * Este método é chamado antes de todos os testCases.
   */
  @BeforeClass
  public static void beforeClass() {
    props = new Properties();
    props.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    props.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
  }

  /**
   * Testa o init passando o Properties null.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test(expected = IllegalArgumentException.class)
  public void initNullProps() throws OpenBusException, UserException {
    Openbus.getInstance().init(null, null, hostName, hostPort);
  }

  /**
   * Testa o init passando ACSHost null.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test(expected = IllegalArgumentException.class)
  public void initNullHost() throws OpenBusException, UserException {
    Openbus.getInstance().init(null, props, null, 0);
  }

  /**
   * Testa o init passando ACSPort null.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test(expected = IllegalArgumentException.class)
  public void initInvalidPort() throws OpenBusException, UserException {
    Openbus.getInstance().init(null, props, hostName, -1);
  }

  /**
   * Testa o init sendo executado duas vezes.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test(expected = OpenbusAlreadyInitializedException.class)
  public void initTwice() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    try {
      openbus.init(null, props, hostName, hostPort);
      openbus.init(null, props, hostName, hostPort);
    }
    finally {
      openbus.destroy();
    }
  }

  /**
   * Testa o init passando um endereço inválido.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test(expected = ACSUnavailableException.class)
  public void initInvalidAddress() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    openbus.init(null, props, "INVALID", hostPort);
    openbus.connect(userLogin, userPassword);
  }

  /**
   * Testa o destroy mesmo que o <i>Openbus</i> não tenha sido inicializado.
   * 
   * @throws OpenBusException
   * @throws UserException
   */
  @Test
  public void destroyWithoutInit() throws OpenBusException, UserException {
    Openbus openbus = Openbus.getInstance();
    openbus.destroy();
  }

}
