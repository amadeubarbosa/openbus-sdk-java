package tecgraf.openbus;

/**
 * Interface com operações para gerenciar acesso multiplexado a diferentes
 * barramentos OpenBus usando um mesmo ORB.
 * 
 * @author Tecgraf
 */
public interface ConnectionManager {

  /**
   * Identificador no INITIAL_REFERENCE de CORBA
   */
  static final String INITIAL_REFERENCE_ID = "openbus.ConnectionMultiplexer";

  /**
   * Recupera o ORB que está associado.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Cria uma conexão para um barramento a partir de um endereço de rede IP e
   * uma porta.
   * 
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * 
   * @return Conexão ao barramento referenciado.
   */
  Connection createConnection(String host, int port);

  /**
   * Define a conexão a ser utilizada nas chamadas realizadas e no despacho de
   * chamadas recebidas sempre que não houver uma conexão específica definida.
   * Sempre que não houver uma conexão associada tanto as chamadas realizadas
   * como as chamadas recebidas são negadas com a exceção CORBA::NO_PERMISSION.
   * 
   * @param conn Conexão a ser definida como conexão default.
   */
  void setDefaultConnection(Connection conn);

  /**
   * Obtém a conexão a ser utilizada nas chamadas realizadas e no despacho de
   * chamadas recebidas sempre que não houver uma conexão específica definida.
   * 
   * @return Conexão definida como conexão default.
   */
  Connection getDefaultConnection();

  /**
   * Define a conexão com o barramento a ser utilizada em todas as chamadas
   * feitas pela thread corrente. Quando 'conn' é <code>null</code> a thread
   * passa a ficar sem nenhuma conexão associada.
   * 
   * @param conn Conexão a barramento a ser associada a thread corrente.
   */
  void setThreadRequester(Connection conn);

  /**
   * Devolve a conexão com o barramento associada a thread corrente, ou
   * <code>null</code> caso não haja nenhuma conexão associada à thread.
   * 
   * @return Conexão a barramento associada a thread corrente.
   */
  Connection getThreadRequester();

  /**
   * Define que conexão deve ser utilizada para receber chamadas oriundas do
   * barramento ao qual está conectada, denominada conexão de despacho.
   * 
   * @param conn Conexão a barramento a ser associada a thread corrente.
   */
  void setupBusDispatcher(Connection conn);

  /**
   * Devolve a conexão de despacho associada ao barramento indicado, se houver.
   * dado barramento, ou 'null' caso não haja nenhuma conexão associada ao
   * barramento.
   * 
   * @param busid Identificador do barramento ao qual a conexão está associada.
   * @return Conexão a barramento associada ao barramento.
   */
  Connection getBusDispatcher(String busid);

  /**
   * Remove a conexão de despacho associada ao barramento indicado, se houver.
   * 
   * @param busid identificador do barramento
   * @return Conexão a barramento associada ao barramento ou 'null' se não
   *         houver nenhuma conexão associada.
   */
  Connection removeBusDispatcher(String busid);
}
