package tecgraf.openbus.test_case;

import org.omg.CORBA.UserException;

import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.launcher.ClientTestContext;

/**
 * Descreve todos os métodos necessários para criar um cliente para testes
 * remotos.
 * 
 * @author Tecgraf
 */
public interface ClientTestCase {

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
  void init(ClientTestContext context) throws OpenBusException, UserException;

  /**
   * <p>
   * Responsável por conectar-se ao barramento.
   * </p>
   * <p>
   * É necessário que este método chame algum Openbus.connect().
   * </p>
   * 
   * @param context O contexto.
   * @throws OpenBusException
   */
  void connect(ClientTestContext context) throws OpenBusException;

  /**
   * <p>
   * Responsável por procurar a oferta no Serviço de Registro.
   * </p>
   * <p>
   * É necessário que este método encontre um serviço válido para ser testado.
   * </p>
   * 
   * @param context O contexto.
   */
  void findOffer(ClientTestContext context);

  /**
   * Responsável por executar os métodos da oferta encontrada.
   * 
   * @param context
   */
  void executeServant(ClientTestContext context);

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
  void disconnect(ClientTestContext context);

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
  void destroy(ClientTestContext context);
}
