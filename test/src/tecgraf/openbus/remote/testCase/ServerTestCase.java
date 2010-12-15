package tecgraf.openbus.remote.testCase;

import org.omg.CORBA.UserException;

import tecgraf.openbus.core.v1_06.registry_service.UnathorizedFacets;
import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.remote.launcher.ServerTestContext;

/**
 * Utilizado para executar eventos entre as chamadas da API Openbus.
 * 
 * @author Tecgraf
 */
public interface ServerTestCase {

  /**
   * <p>
   * Responsável por inicializar a classe Openbus.
   * </p>
   * <p>
   * É necessário que este método chame o Openbus.init().
   * </p>
   * 
   * @param context O contexto.
   * @throws UserException
   * @throws OpenBusException
   */
  void init(ServerTestContext context) throws OpenBusException, UserException;

  /**
   * <p>
   * Responsável por conectar-se ao barramento.
   * </p>
   * <p>
   * É necessário que este método chame o Openbus.connect() por certificado.
   * </p>
   * 
   * @param context O contexto.
   * @throws OpenBusException
   */
  void connect(ServerTestContext context) throws OpenBusException;

  /**
   * <p>
   * Responsável por criar o componente.
   * </p>
   * 
   * @param context O contexto.
   * @throws Exception
   */
  public void createComponent(ServerTestContext context) throws Exception;

  /**
   * <p>
   * Responsável por disponibilizar a oferta no Serviço de Registro.
   * </p>
   * <p>
   * É necessário que o este método encontre um serviço válido para ser testado.
   * </p>
   * 
   * @param context O contexto.
   * @return O registrationId (resposta do método register do Serviço de
   *         Registro)
   * @throws UnathorizedFacets
   */
  public String registerComponent(ServerTestContext context)
    throws UnathorizedFacets;

  /**
   * <p>
   * Responsável pela desconexão do barramento.
   * </p>
   * <p>
   * É necessário que este método chame o Openbus.disconnect().
   * </p>
   * 
   * @param context
   */
  void disconnect(ServerTestContext context);

  /**
   * <p>
   * Responsável por destruir a classe Openbus.
   * </p>
   * <p>
   * É necessário que este método chame o Openbus.destroy().
   * </p>
   * 
   * @param context
   */
  void destroy(ServerTestContext context);
}
