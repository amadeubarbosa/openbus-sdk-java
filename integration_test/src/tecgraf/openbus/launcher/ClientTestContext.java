package tecgraf.openbus.launcher;

import java.util.Properties;

/**
 * Contexto do cliente para os testes remotos.
 * 
 * @author Tecgraf
 */
public class ClientTestContext {

  /**
   * Propriedades necessárias para o Openbus
   */
  public Properties properties;

  /**
   * O serviço que será utilizado no teste.
   */
  public org.omg.CORBA.Object servant;

}
