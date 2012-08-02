package tecgraf.openbus;

import java.util.Properties;

import org.omg.PortableInterceptor.Current;

import tecgraf.openbus.exception.InvalidBusAddress;
import tecgraf.openbus.exception.InvalidPropertyValue;

/**
 * Gerencia conexões de acesso a barramentos OpenBus através de um ORB.
 * <p>
 * Conexões representam formas diferentes de acesso ao barramento. O
 * ConnectionManager permite criar essas conexões e gerenciá-las, indicando
 * quais são utilizadas em cada chamada. As conexões são usadas basicamente de
 * duas formas no tratamento das chamadas:
 * <ul>
 * <li>para realizar uma chamada remota (cliente), neste caso a conexão é
 * denominada "Requester".
 * <li>para validar uma chamada recebida (servidor), neste caso a conexão é
 * denominada "Dispatcher".
 * </ul>
 * 
 * @author Tecgraf
 */
public interface ConnectionManager {

  /**
   * Identificador no INITIAL_REFERENCE de CORBA do ConnectionManager.
   */
  static final String INITIAL_REFERENCE_ID = "OpenBusConnectionManager";

  /**
   * Recupera o ORB associado ao ConnectionManager.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Cria uma conexão para um barramento. O barramento é indicado por um nome ou
   * endereço de rede e um número de porta, onde os serviços núcleo daquele
   * barramento estão executando.
   * 
   * @param host Endereço ou nome de rede onde os serviços núcleo do barramento
   *        estão executando.
   * @param port Porta onde os serviços núcleo do barramento estão executando.
   * 
   * @return Conexão criada.
   * 
   * @throws InvalidBusAddress Os parâmetros 'host' e 'port' não são válidos.
   */
  Connection createConnection(String host, int port) throws InvalidBusAddress;

  /**
   * Cria uma conexão para um barramento. O barramento é indicado por um nome ou
   * endereço de rede e um número de porta, onde os serviços núcleo daquele
   * barramento estão executando.
   * 
   * @param host Endereço ou nome de rede onde os serviços núcleo do barramento
   *        estão executando.
   * @param port Porta onde os serviços núcleo do barramento estão executando.
   * @param props Lista opcional de propriedades que definem algumas
   *        configurações sobre a forma que as chamadas realizadas ou validadas
   *        com essa conexão são feitas. A seguir são listadas as propriedades
   *        válidas:
   *        <ul>
   *        <li>access.key: chave de acesso a ser utiliza internamente para a
   *        geração de credenciais que identificam as chamadas através do
   *        barramento. A chave deve ser uma chave privada RSA de 2048 bits (256
   *        bytes). Quando essa propriedade não é fornecida, uma chave de acesso
   *        é gerada automaticamente.
   *        <li>legacy.disable: desabilita o suporte a chamadas usando protocolo
   *        OpenBus 1.5. Por padrão o suporte está habilitado. Valores esperados
   *        são <code>true</code> ou <code>false</code>.
   *        <li>legacy.delegate: indica como é preenchido o campo 'delegate' das
   *        credenciais enviadas em chamadas usando protocolo OpenBus 1.5. Há
   *        duas formas possíveis (o padrão é 'caller'):
   *        <ul>
   *        <li>caller: o campo 'delegate' é preenchido sempre com a entidade do
   *        campo 'caller' da cadeia de chamadas.
   *        <li>originator: o campo 'delegate' é preenchido sempre com a
   *        entidade que originou a cadeia de chamadas, que é o primeiro login
   *        do campo 'originators' ou o campo 'caller' quando este é vazio.
   *        </ul>
   *        </ul>
   * 
   * @return Conexão criada.
   * 
   * @throws InvalidBusAddress Os parâmetros 'host' e 'port' não são válidos.
   * @throws InvalidPropertyValue O valor de uma propriedade não é válido.
   */
  Connection createConnection(String host, int port, Properties props)
    throws InvalidBusAddress, InvalidPropertyValue;

  /**
   * Define a conexão padrão a ser usada nas chamadas.
   * <p>
   * Define uma conexão a ser utilizada como "Requester" e "Dispatcher" de
   * chamadas sempre que não houver uma conexão "Requester" e "Dispatcher"
   * específica definida para o caso específico, como é feito através das
   * operações {@link ConnectionManager#setRequester(Connection) setRequester} e
   * {@link ConnectionManager#setDispatcher(Connection) setDispatcher}.
   * 
   * @param conn Conexão a ser definida como conexão padrão.
   */
  void setDefaultConnection(Connection conn);

  /**
   * Devolve a conexão padrão.
   * <p>
   * Veja operação {@link ConnectionManager#setDefaultConnection
   * setDefaultConnection}.
   * 
   * @return Conexão definida como conexão padrão.
   */
  Connection getDefaultConnection();

  /**
   * Define a conexão "Requester" do contexto corrente.
   * <p>
   * Define a conexão "Requester" a ser utilizada em todas as chamadas feitas no
   * contexto atual, por exemplo, o contexto representado pelo {@link Current}
   * atual. Quando <code>conn</code> é <code>null</code> o contexto passa a
   * ficar sem nenhuma conexão associada.
   * 
   * @param conn Conexão a ser associada ao contexto corrente.
   */
  void setRequester(Connection conn);

  /**
   * Devolve a conexão associada ao contexto corrente.
   * 
   * @return Conexão associada ao contexto corrente, ou <code>null</code> caso
   *         não haja nenhuma conexão associada.
   */
  Connection getRequester();

  /**
   * Define uma a conexão como "Dispatcher" de barramento.
   * <p>
   * Define a conexão como "Dispatcher" do barramento ao qual ela está
   * conectada, de forma que todas as chamadas originadas por entidades
   * conectadas a este barramento serão validadas com essa conexão. Só pode
   * haver uma conexão "Dispatcher" para cada barramento, portanto se já houver
   * outra conexão "Dispatcher" para o mesmo barramento essa será substituída
   * pela nova conexão.
   * 
   * @param conn Conexão a ser definida como "Dispatcher".
   */
  void setDispatcher(Connection conn);

  /**
   * Devolve a conexão "Dispatcher" do barramento indicado.
   * 
   * @param busid Identificador do barramento ao qual a conexão está associada.
   * 
   * @return Conexão "Dispatcher" do barramento indicado, ou <code>null</code>
   *         caso não haja nenhuma conexão "Dispatcher" associada ao barramento
   *         indicado.
   */
  Connection getDispatcher(String busid);

  /**
   * Remove a conexão "Dispatcher" associada ao barramento indicado.
   * 
   * @param busid Identificador do barramento ao qual a conexão está associada.
   * 
   * @return Conexão "Dispatcher" associada ao barramento ou <code>null</code>
   *         se não houver nenhuma conexão associada.
   */
  Connection clearDispatcher(String busid);
}
