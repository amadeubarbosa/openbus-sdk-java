package tecgraf.openbus;

/**
 * Interface com opera��es para gerenciar acesso multiplexado a diferentes
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
   * Recupera o ORB que est� associado.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Cria uma conex�o para um barramento a partir de um endere�o de rede IP e
   * uma porta.
   * 
   * @param host Endere�o de rede IP onde o barramento est� executando.
   * @param port Porta do processo do barramento no endere�o indicado.
   * 
   * @return Conex�o ao barramento referenciado.
   */
  Connection createConnection(String host, int port);

  /**
   * Define a conex�o a ser utilizada nas chamadas realizadas e no despacho de
   * chamadas recebidas sempre que n�o houver uma conex�o espec�fica definida.
   * Sempre que n�o houver uma conex�o associada tanto as chamadas realizadas
   * como as chamadas recebidas s�o negadas com a exce��o CORBA::NO_PERMISSION.
   * 
   * @param conn Conex�o a ser definida como conex�o default.
   */
  void setDefaultConnection(Connection conn);

  /**
   * Obt�m a conex�o a ser utilizada nas chamadas realizadas e no despacho de
   * chamadas recebidas sempre que n�o houver uma conex�o espec�fica definida.
   * 
   * @return Conex�o definida como conex�o default.
   */
  Connection getDefaultConnection();

  /**
   * Define a conex�o com o barramento a ser utilizada em todas as chamadas
   * feitas pela thread corrente. Quando 'conn' � <code>null</code> a thread
   * passa a ficar sem nenhuma conex�o associada.
   * 
   * @param conn Conex�o a barramento a ser associada a thread corrente.
   */
  void setThreadRequester(Connection conn);

  /**
   * Devolve a conex�o com o barramento associada a thread corrente, ou
   * <code>null</code> caso n�o haja nenhuma conex�o associada � thread.
   * 
   * @return Conex�o a barramento associada a thread corrente.
   */
  Connection getThreadRequester();

  /**
   * Define que conex�o deve ser utilizada para receber chamadas oriundas do
   * barramento ao qual est� conectada, denominada conex�o de despacho.
   * 
   * @param conn Conex�o a barramento a ser associada a thread corrente.
   */
  void setupBusDispatcher(Connection conn);

  /**
   * Devolve a conex�o de despacho associada ao barramento indicado, se houver.
   * dado barramento, ou 'null' caso n�o haja nenhuma conex�o associada ao
   * barramento.
   * 
   * @param busid Identificador do barramento ao qual a conex�o est� associada.
   * @return Conex�o a barramento associada ao barramento.
   */
  Connection getBusDispatcher(String busid);

  /**
   * Remove a conex�o de despacho associada ao barramento indicado, se houver.
   * 
   * @param busid identificador do barramento
   * @return Conex�o a barramento associada ao barramento ou 'null' se n�o
   *         houver nenhuma conex�o associada.
   */
  Connection removeBusDispatcher(String busid);
}
