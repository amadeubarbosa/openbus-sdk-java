package tecgraf.openbus.launcher;

import java.util.Properties;

import scs.core.servant.ComponentContext;

/**
 * Contexto do cliente para os testes remotos.
 * 
 * @author Tecgraf
 */
public class ServerTestContext {

  /**
   * Propriedades necess�rias para o Openbus.
   */
  public Properties properties;

  /**
   * O componentContext que ser� registrado no barramento.
   */
  public ComponentContext componentContext;

}
