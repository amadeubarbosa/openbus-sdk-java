package tecgraf.openbus.test_case;

import org.omg.CORBA.UserException;

import tecgraf.openbus.exception.OpenBusException;
import tecgraf.openbus.launcher.ClientTestContext;

/**
 * Descreve todos os m�todos necess�rios para criar um cliente para testes
 * remotos.
 * 
 * @author Tecgraf
 */
public interface ClientTestCase {

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
  void init(ClientTestContext context) throws OpenBusException, UserException;

  /**
   * <p>
   * Respons�vel por conectar-se ao barramento.
   * </p>
   * <p>
   * � necess�rio que este m�todo chame algum Openbus.connect().
   * </p>
   * 
   * @param context O contexto.
   * @throws OpenBusException
   */
  void connect(ClientTestContext context) throws OpenBusException;

  /**
   * <p>
   * Respons�vel por procurar a oferta no Servi�o de Registro.
   * </p>
   * <p>
   * � necess�rio que este m�todo encontre um servi�o v�lido para ser testado.
   * </p>
   * 
   * @param context O contexto.
   */
  void findOffer(ClientTestContext context);

  /**
   * Respons�vel por executar os m�todos da oferta encontrada.
   * 
   * @param context
   */
  void executeServant(ClientTestContext context);

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
  void disconnect(ClientTestContext context);

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
  void destroy(ClientTestContext context);
}
