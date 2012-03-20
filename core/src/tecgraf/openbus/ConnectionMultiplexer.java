package tecgraf.openbus;

import org.omg.CORBA.NO_PERMISSION;

/**
 * Interface com operações para gerenciar acesso multiplexado a diferentes
 * barramentos OpenBus usando um mesmo ORB.
 * 
 * @author Tecgraf
 */
public interface ConnectionMultiplexer {

  /**
   * Identificador no INITIAL_REFERENCE de CORBA
   */
  static final String INITIAL_REFERENCE_ID = "openbus.ConnectionMultiplexer";

  /**
   * Devolve todas as conexões sendo multiplexadas através do ORB.
   * 
   * @return Sequência de conexões sendo multiplexadas através do ORB.
   */
  Connection[] getConnections();

  /**
   * Define a conexão com o barramento a ser utilizada em todas as chamadas
   * feitas pela thread corrente. Quando 'conn' é <code>null</code> a thread
   * passa a ficar sem nenhuma conexão associada.
   * 
   * @param conn Conexão a barramento a ser associada a thread corrente.
   */
  void setCurrentConnection(Connection conn);

  /**
   * Devolve a conexão com o barramento associada a thread corrente, ou
   * <code>null</code> caso não haja nenhuma conexão associada à thread.
   * 
   * @return Conexão a barramento associada a thread corrente.
   */
  Connection getCurrentConnection();

  /**
   * Define a conexão a ser utilizada para recerber chamadas oriundas de um dado
   * barramento. Quando 'conn' é <code>null</code> o barramento passa a ficar
   * sem nenhuma conexão associada. Sempre que o barramento não possui uma
   * conexão associada todas as chamadas oriundas daquele barramento são negadas
   * ({@link NO_PERMISSION}).
   * 
   * @param busid Identificador do barramento ao qual a conexão será associada.
   * @param conn Conexão a barramento a ser associada a thread corrente.
   */
  void setIncommingConnection(String busid, Connection conn);

  /**
   * Devolve a conexão a ser utilizada para recerber chamadas oriundas de um
   * dado barramento, ou <code>null</code> caso não haja nenhuma conexão
   * associada ao barramento.
   * 
   * @param busid Identificador do barramento ao qual a conexão está associada.
   * @return Conexão a barramento associada ao barramento.
   */
  Connection getIncommingConnection(String busid);
}
