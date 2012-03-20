package tecgraf.openbus;

import org.omg.CORBA.NO_PERMISSION;

/**
 * Interface com opera��es para gerenciar acesso multiplexado a diferentes
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
   * Devolve todas as conex�es sendo multiplexadas atrav�s do ORB.
   * 
   * @return Sequ�ncia de conex�es sendo multiplexadas atrav�s do ORB.
   */
  Connection[] getConnections();

  /**
   * Define a conex�o com o barramento a ser utilizada em todas as chamadas
   * feitas pela thread corrente. Quando 'conn' � <code>null</code> a thread
   * passa a ficar sem nenhuma conex�o associada.
   * 
   * @param conn Conex�o a barramento a ser associada a thread corrente.
   */
  void setCurrentConnection(Connection conn);

  /**
   * Devolve a conex�o com o barramento associada a thread corrente, ou
   * <code>null</code> caso n�o haja nenhuma conex�o associada � thread.
   * 
   * @return Conex�o a barramento associada a thread corrente.
   */
  Connection getCurrentConnection();

  /**
   * Define a conex�o a ser utilizada para recerber chamadas oriundas de um dado
   * barramento. Quando 'conn' � <code>null</code> o barramento passa a ficar
   * sem nenhuma conex�o associada. Sempre que o barramento n�o possui uma
   * conex�o associada todas as chamadas oriundas daquele barramento s�o negadas
   * ({@link NO_PERMISSION}).
   * 
   * @param busid Identificador do barramento ao qual a conex�o ser� associada.
   * @param conn Conex�o a barramento a ser associada a thread corrente.
   */
  void setIncommingConnection(String busid, Connection conn);

  /**
   * Devolve a conex�o a ser utilizada para recerber chamadas oriundas de um
   * dado barramento, ou <code>null</code> caso n�o haja nenhuma conex�o
   * associada ao barramento.
   * 
   * @param busid Identificador do barramento ao qual a conex�o est� associada.
   * @return Conex�o a barramento associada ao barramento.
   */
  Connection getIncommingConnection(String busid);
}
