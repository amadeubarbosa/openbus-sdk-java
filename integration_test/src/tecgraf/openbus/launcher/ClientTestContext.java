package tecgraf.openbus.launcher;

import java.util.Properties;

/**
 * Contexto do cliente para os testes remotos.
 * 
 * @author Tecgraf
 */
public class ClientTestContext {

  /**
   * Propriedades necess�rias para o Openbus
   */
  public Properties properties;

  /**
   * O servi�o que ser� utilizado no teste.
   */
  public org.omg.CORBA.Object servant;

}
