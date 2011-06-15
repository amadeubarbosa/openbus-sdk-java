package tecgraf.openbus.test_case;

import org.omg.CORBA.UserException;

import tecgraf.openbus.core.v1_06.registry_service.UnauthorizedFacets;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.launcher.ServerTestContext;

/**
 * Utilizado para executar eventos entre as chamadas da API Openbus.
 * 
 * @author Tecgraf
 */
public interface ServerTestCase {

  /**
   * <p>
   * Respons�vel por inicializar a classe Openbus.
   * </p>
   * <p>
   * � necess�rio que este m�todo chame o Openbus.init().
   * </p>
   * 
   * @param context O contexto.
   * @throws UserException
   * @throws OpenBusException
   */
  void init(ServerTestContext context) throws OpenBusException, UserException;

  /**
   * <p>
   * Respons�vel por conectar-se ao barramento.
   * </p>
   * <p>
   * � necess�rio que este m�todo chame o Openbus.connect() por certificado.
   * </p>
   * 
   * @param context O contexto.
   * @throws OpenBusException
   */
  void connect(ServerTestContext context) throws OpenBusException;

  /**
   * <p>
   * Respons�vel por criar o componente.
   * </p>
   * 
   * @param context O contexto.
   * @throws Exception
   */
  public void createComponent(ServerTestContext context) throws Exception;

  /**
   * <p>
   * Respons�vel por disponibilizar a oferta no Servi�o de Registro.
   * </p>
   * <p>
   * � necess�rio que o este m�todo encontre um servi�o v�lido para ser testado.
   * </p>
   * 
   * @param context O contexto.
   * @return O registrationId (resposta do m�todo register do Servi�o de
   *         Registro)
   * @throws UnauthorizedFacets
   */
  public String registerComponent(ServerTestContext context)
    throws UnauthorizedFacets;

  /**
   * <p>
   * Respons�vel pela desconex�o do barramento.
   * </p>
   * <p>
   * � necess�rio que este m�todo chame o Openbus.disconnect().
   * </p>
   * 
   * @param context
   */
  void disconnect(ServerTestContext context);

  /**
   * <p>
   * Respons�vel por destruir a classe Openbus.
   * </p>
   * <p>
   * � necess�rio que este m�todo chame o Openbus.destroy().
   * </p>
   * 
   * @param context
   */
  void destroy(ServerTestContext context);
}
