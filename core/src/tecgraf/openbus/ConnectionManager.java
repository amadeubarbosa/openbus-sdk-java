package tecgraf.openbus;

import java.util.Properties;

import org.omg.PortableInterceptor.Current;

import tecgraf.openbus.exception.InvalidBusAddress;
import tecgraf.openbus.exception.InvalidPropertyValue;

/**
 * Gerencia conex�es de acesso a barramentos OpenBus atrav�s de um ORB.
 * <p>
 * Conex�es representam formas diferentes de acesso ao barramento. O
 * ConnectionManager permite criar essas conex�es e gerenci�-las, indicando
 * quais s�o utilizadas em cada chamada. As conex�es s�o usadas basicamente de
 * duas formas no tratamento das chamadas:
 * <ul>
 * <li>para realizar uma chamada remota (cliente), neste caso a conex�o �
 * denominada "Requester".
 * <li>para validar uma chamada recebida (servidor), neste caso a conex�o �
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
   * Cria uma conex�o para um barramento. O barramento � indicado por um nome ou
   * endere�o de rede e um n�mero de porta, onde os servi�os n�cleo daquele
   * barramento est�o executando.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * 
   * @return Conex�o criada.
   * 
   * @throws InvalidBusAddress Os par�metros 'host' e 'port' n�o s�o v�lidos.
   */
  Connection createConnection(String host, int port) throws InvalidBusAddress;

  /**
   * Cria uma conex�o para um barramento. O barramento � indicado por um nome ou
   * endere�o de rede e um n�mero de porta, onde os servi�os n�cleo daquele
   * barramento est�o executando.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * @param props Lista opcional de propriedades que definem algumas
   *        configura��es sobre a forma que as chamadas realizadas ou validadas
   *        com essa conex�o s�o feitas. A seguir s�o listadas as propriedades
   *        v�lidas:
   *        <ul>
   *        <li>access.key: chave de acesso a ser utiliza internamente para a
   *        gera��o de credenciais que identificam as chamadas atrav�s do
   *        barramento. A chave deve ser uma chave privada RSA de 2048 bits (256
   *        bytes). Quando essa propriedade n�o � fornecida, uma chave de acesso
   *        � gerada automaticamente.
   *        <li>legacy.disable: desabilita o suporte a chamadas usando protocolo
   *        OpenBus 1.5. Por padr�o o suporte est� habilitado. Valores esperados
   *        s�o <code>true</code> ou <code>false</code>.
   *        <li>legacy.delegate: indica como � preenchido o campo 'delegate' das
   *        credenciais enviadas em chamadas usando protocolo OpenBus 1.5. H�
   *        duas formas poss�veis (o padr�o � 'caller'):
   *        <ul>
   *        <li>caller: o campo 'delegate' � preenchido sempre com a entidade do
   *        campo 'caller' da cadeia de chamadas.
   *        <li>originator: o campo 'delegate' � preenchido sempre com a
   *        entidade que originou a cadeia de chamadas, que � o primeiro login
   *        do campo 'originators' ou o campo 'caller' quando este � vazio.
   *        </ul>
   *        </ul>
   * 
   * @return Conex�o criada.
   * 
   * @throws InvalidBusAddress Os par�metros 'host' e 'port' n�o s�o v�lidos.
   * @throws InvalidPropertyValue O valor de uma propriedade n�o � v�lido.
   */
  Connection createConnection(String host, int port, Properties props)
    throws InvalidBusAddress, InvalidPropertyValue;

  /**
   * Define a conex�o padr�o a ser usada nas chamadas.
   * <p>
   * Define uma conex�o a ser utilizada como "Requester" e "Dispatcher" de
   * chamadas sempre que n�o houver uma conex�o "Requester" e "Dispatcher"
   * espec�fica definida para o caso espec�fico, como � feito atrav�s das
   * opera��es {@link ConnectionManager#setRequester(Connection) setRequester} e
   * {@link ConnectionManager#setDispatcher(Connection) setDispatcher}.
   * 
   * @param conn Conex�o a ser definida como conex�o padr�o.
   */
  void setDefaultConnection(Connection conn);

  /**
   * Devolve a conex�o padr�o.
   * <p>
   * Veja opera��o {@link ConnectionManager#setDefaultConnection
   * setDefaultConnection}.
   * 
   * @return Conex�o definida como conex�o padr�o.
   */
  Connection getDefaultConnection();

  /**
   * Define a conex�o "Requester" do contexto corrente.
   * <p>
   * Define a conex�o "Requester" a ser utilizada em todas as chamadas feitas no
   * contexto atual, por exemplo, o contexto representado pelo {@link Current}
   * atual. Quando <code>conn</code> � <code>null</code> o contexto passa a
   * ficar sem nenhuma conex�o associada.
   * 
   * @param conn Conex�o a ser associada ao contexto corrente.
   */
  void setRequester(Connection conn);

  /**
   * Devolve a conex�o associada ao contexto corrente.
   * 
   * @return Conex�o associada ao contexto corrente, ou <code>null</code> caso
   *         n�o haja nenhuma conex�o associada.
   */
  Connection getRequester();

  /**
   * Define uma a conex�o como "Dispatcher" de barramento.
   * <p>
   * Define a conex�o como "Dispatcher" do barramento ao qual ela est�
   * conectada, de forma que todas as chamadas originadas por entidades
   * conectadas a este barramento ser�o validadas com essa conex�o. S� pode
   * haver uma conex�o "Dispatcher" para cada barramento, portanto se j� houver
   * outra conex�o "Dispatcher" para o mesmo barramento essa ser� substitu�da
   * pela nova conex�o.
   * 
   * @param conn Conex�o a ser definida como "Dispatcher".
   */
  void setDispatcher(Connection conn);

  /**
   * Devolve a conex�o "Dispatcher" do barramento indicado.
   * 
   * @param busid Identificador do barramento ao qual a conex�o est� associada.
   * 
   * @return Conex�o "Dispatcher" do barramento indicado, ou <code>null</code>
   *         caso n�o haja nenhuma conex�o "Dispatcher" associada ao barramento
   *         indicado.
   */
  Connection getDispatcher(String busid);

  /**
   * Remove a conex�o "Dispatcher" associada ao barramento indicado.
   * 
   * @param busid Identificador do barramento ao qual a conex�o est� associada.
   * 
   * @return Conex�o "Dispatcher" associada ao barramento ou <code>null</code>
   *         se n�o houver nenhuma conex�o associada.
   */
  Connection clearDispatcher(String busid);
}
