package tecgraf.openbus;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.Current;

import org.omg.PortableServer.POA;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.exception.InvalidEncodedStream;
import tecgraf.openbus.exception.InvalidPropertyValue;

/**
 * Permite controlar o contexto das chamadas de um {@link ORB} para acessar
 * informa��es que identificam essas chamadas em barramentos OpenBus.
 * <p>
 * O contexto de uma chamada pode ser definido pela linha de execu��o atual do
 * programa em que executa uma chamada, o que pode ser a <i>thread</i> em
 * execu��o ou mais comumente o {@link Current} do padr�o CORBA. As
 * informa��es acess�veis atrav�s do {@link OpenBusContext} se referem
 * basicamente � identifica��o da origem das chamadas, ou seja, o nome das
 * entidades que autenticaram os acessos ao barramento que originaram as
 * chamadas.
 * <p>
 * A identifica��o de chamadas no barramento � controlada no OpenBusContext
 * atrav�s da manipula��o de duas abstra��es representadas pelas seguintes
 * interfaces:
 * <ul>
 * <li> {@link Connection}: Representa um acesso ao barramento, que � usado
 * tanto para fazer chamadas como para receber chamadas atrav�s do barramento
 * . Para tal a conex�o precisa estar autenticada, ou seja, ter um login. Cada
 * chamada feita atrav�s do ORB � enviada com as informa��es do login da conex�o
 * associada ao contexto em que a chamada foi realizada. Cada chamada recebida
 * tamb�m deve vir atrav�s de uma conex�o com login, que deve ser o mesmo login
 * com que chamadas aninhadas a essa chamada original devem ser feitas.
 * <li> {@link CallChain}: Representa a identifica��o de todos os acessos ao
 * barramento que originaram uma chamada recebida. Sempre que uma chamada �
 * recebida e executada, � poss�vel obter uma cadeia de chamadas atrav�s da
 * qual � poss�vel inspecionar as informa��es de acesso que originaram a chamada
 * recebida.
 * </ul>
 * 
 * @author Tecgraf
 */
public interface OpenBusContext {

  /**
   * Fornece o ORB associado ao OpenBusContext.
   * 
   * @return O ORB.
   */
  org.omg.CORBA.ORB ORB();

  /**
   * Fornece o POA associado ao OpenBusContext. Inicialmente o contexto
   * utiliza o RootPOA, mas � poss�vel alterar o POA atrav�s do m�todo
   * {@link #POA(POA)}.
   *
   * @return O POA a ser utilizado ao criar conex�es.
   */
  POA POA();

  /**
   * Configura o POA que o OpenBusContext utilizar� ao criar conex�es.
   *
   * @param poa O POA a ser utilizado.
   */
  void POA(POA poa);

  /**
   * <i>Callback</i> a ser chamada para determinar a conex�o a ser utilizada
   * para receber cada chamada.
   * <p>
   * Essa callback deve fornecer a conex�o a ser utilizada para para receber
   * uma chamada. Essa conex�o ser� a �nica conex�o atrav�s da qual novas
   * chamadas aninhadas � chamada recebida poder�o ser feitas (veja a
   * opera��o {@link #joinChain}).
   * <p>
   * Se a <i>callback</i> estiver definida como {@code null} e houver uma
   * conex�o padr�o definida, a conex�o padr�o ser� utilizada para receber a
   * chamada. Caso ambos sejam {@code null}, a chamada n�o poder� ser
   * atendida e retornar� um erro para o cliente remoto.
   *
   * @param callback Objeto que implementa a interface de <i>callback</i> ou
   * {@code null} para remover o objeto atual.
   */
  void onCallDispatch(CallDispatchCallback callback);

  /**
   * Fornece a callback configurada para receber chamadas.
   * 
   * @return A callback ou {@code null} caso ela n�o exista.
   */
  CallDispatchCallback onCallDispatch();

  /**
   * Cria uma conex�o para um barramento. O barramento � definido por um nome ou
   * endere�o de rede e um n�mero de porta, onde os servi�os n�cleo daquele
   * barramento est�o executando.
   *
   * Esta chamada n�o implica em chamadas remotas, apenas realiza a
   * configura��o necess�ria para acessar um barramento e realizar
   * autentica��es.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * 
   * @return A conex�o criada.
   */
  Connection connectByAddress(String host, int port);

  /**
   * Cria uma conex�o para um barramento. O barramento � definido por um nome ou
   * endere�o de rede e um n�mero de porta, onde os servi�os n�cleo daquele
   * barramento est�o executando.
   *
   * Esta chamada n�o implica em chamadas remotas, apenas realiza a
   * configura��o necess�ria para acessar um barramento e realizar
   * autentica��es.
   *
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * @param props Lista opcional de propriedades que definem algumas
   *        configura��es sobre a forma que as chamadas realizadas ou validadas
   *        com essa conex�o s�o feitas. As propriedades v�lidas est�o
   *        definidas em {@link tecgraf.openbus.core.OpenBusProperty}.
   * 
   * @return A conex�o criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade n�o � v�lido.
   */
  Connection connectByAddress(String host, int port, Properties props)
    throws InvalidPropertyValue;

  /**
   * Cria uma conex�o para um barramento. O barramento � definido por uma
   * refer�ncia CORBA a um componente SCS que representa os servi�os n�cleo do
   * barramento. Essa fun��o deve ser utilizada ao inv�s da
   * {@link #connectByAddress} para permitir o uso de SSL nas
   * comunica��es com o n�cleo do barramento.
   *
   * Esta chamada n�o implica em chamadas remotas, apenas realiza a
   * configura��o necess�ria para acessar um barramento e realizar
   * autentica��es.
   *
   * @param reference Refer�ncia CORBA a um componente SCS que representa os
   *        servi�os n�cleo do barramento.
   * 
   * @return A conex�o criada.
   */
  Connection connectByReference(org.omg.CORBA.Object reference);

  /**
   * Cria uma conex�o para um barramento. O barramento � definido por uma
   * refer�ncia CORBA a um componente SCS que representa os servi�os n�cleo do
   * barramento. Essa fun��o deve ser utilizada ao inv�s da
   * {@link #connectByAddress} para permitir o uso de SSL nas
   * comunica��es com o n�cleo do barramento.
   *
   * Esta chamada n�o implica em chamadas remotas, apenas realiza a
   * configura��o necess�ria para acessar um barramento e realizar
   * autentica��es.
   *
   * @param reference Refer�ncia CORBA a um componente SCS que representa os
   *        servi�os n�cleo do barramento.
   * @param props Lista opcional de propriedades que definem algumas
   *        configura��es sobre a forma que as chamadas realizadas ou validadas
   *        com essa conex�o s�o feitas. As propriedades v�lidas est�o
   *        definidas em {@link tecgraf.openbus.core.OpenBusProperty}.
   * 
   * @return A conex�o criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade n�o � v�lido.
   */
  Connection connectByReference(org.omg.CORBA.Object reference, Properties props)
    throws InvalidPropertyValue;

  /**
   * Cria uma conex�o para um barramento. O barramento � definido por um nome ou
   * endere�o de rede e um n�mero de porta, onde os servi�os n�cleo daquele
   * barramento est�o executando.
   *
   * Esta chamada n�o implica em chamadas remotas, apenas realiza a
   * configura��o necess�ria para acessar um barramento e realizar
   * autentica��es.
   *
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * 
   * @return A conex�o criada.
   */
  @Deprecated
  Connection createConnection(String host, int port);

  /**
   * Cria uma conex�o para um barramento. O barramento � definido por um nome ou
   * endere�o de rede e um n�mero de porta, onde os servi�os n�cleo daquele
   * barramento est�o executando.
   *
   * Esta chamada n�o implica em chamadas remotas, apenas realiza a
   * configura��o necess�ria para acessar um barramento e realizar
   * autentica��es.
   *
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * @param props Lista opcional de propriedades que definem algumas
   *        configura��es sobre a forma que as chamadas realizadas ou validadas
   *        com essa conex�o s�o feitas. As propriedades v�lidas est�o
   *        definidas em {@link tecgraf.openbus.core.OpenBusProperty}.
   * 
   * @return A conex�o criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade n�o � v�lido.
   */
  @Deprecated
  Connection createConnection(String host, int port, Properties props)
    throws InvalidPropertyValue;

  /**
   * Define a conex�o padr�o a ser utilizada em chamadas sempre que n�o houver
   * uma conex�o espec�fica definida no contexto atual, como � feito atrav�s das
   * opera��es {@link #currentConnection(Connection)} e
   * {@link #onCallDispatch(CallDispatchCallback)}.
   *
   * A conex�o padr�o n�o afeta chamadas feitas por recursos de uma conex�o.
   * 
   * @param connection A conex�o a ser definida como conex�o padr�o.
   * 
   * @return A conex�o definida como conex�o padr�o anteriormente, ou
   *         {@code null} se n�o havia conex�o padr�o definida.
   */
  Connection defaultConnection(Connection connection);

  /**
   * Fornece a conex�o padr�o a ser usada nas chamadas. Veja opera��o
   * {@link #defaultConnection defaultConnection}.
   *
   * A conex�o padr�o n�o afeta chamadas feitas por recursos de uma conex�o.
   *
   * @return A conex�o definida como conex�o padr�o.
   */
  Connection defaultConnection();

  /**
   * Define a conex�o a ser utilizada em todas as chamadas feitas no contexto
   * atual.
   *
   * Se a conex�o corrente estiver definida como {@code null} e houver uma
   * conex�o padr�o definida, a conex�o padr�o ser� utilizada para realizar
   * chamadas. Caso ambas sejam {@code null}, chamadas n�o poder�o ser
   * realizadas e um erro ser� lan�ado para a aplica��o.
   *
   * A conex�o corrente n�o afeta chamadas feitas por recursos de uma conex�o.
   *
   * @param connection A conex�o a ser associada ao contexto corrente.
   * 
   * @return A conex�o definida como a conex�o corrente anteriormente, ou
   * {@code null} se n�o havia conex�o definida.
   */
  Connection currentConnection(Connection connection);

  /**
   * Fornece a conex�o associada ao contexto corrente e que ser� utilizada em
   * chamadas. Ela pode ter sido definida pela opera��o
   * {@link #currentConnection(Connection)} ou
   * {@link #defaultConnection(Connection)}.
   *
   * A conex�o padr�o n�o afeta chamadas feitas por recursos de uma conex�o.
   *
   * @return A conex�o associada ao contexto corrente, ou {@code null} caso
   *         n�o haja nenhuma conex�o associada.
   */
  Connection currentConnection();

  /**
   * Fornece a cadeia de chamadas � qual a execu��o corrente pertence.
   * <p>
   * Essa opera��o devolve um objeto que representa a cadeia de chamadas do
   * barramento que esta chamada faz parte.
   *
   * @return A cadeia da chamada em execu��o.
   */
  CallerChain callerChain();

  /**
   * Associa uma cadeia de chamadas ao contexto corrente (e.g. definido pelo
   * {@link Current}), de forma que todas as chamadas remotas seguintes neste
   * mesmo contexto sejam feitas como parte dessa cadeia de chamadas.
   *
   * @param chain A cadeia de chamadas a ser associada ao contexto corrente.
   */
  void joinChain(CallerChain chain);

  /**
   * Associa a cadeia de chamadas obtida em {@link #callerChain()} ao
   * contexto corrente (e.g. definido pelo {@link Current}), de forma que todas
   * as chamadas remotas seguintes neste mesmo contexto sejam feitas como parte
   * dessa cadeia de chamadas.
   */
  void joinChain();

  /**
   * Remove a associa��o da cadeia de chamadas ao contexto corrente (e.g.
   * definido pelo {@link Current}), fazendo com que todas as chamadas seguintes
   * feitas neste mesmo contexto deixem de fazer parte da cadeia de chamadas
   * associada previamente. Ou seja, todas as chamadas passam a iniciar novas
   * cadeias de chamada.
   */
  void exitChain();

  /**
   * Fornece a cadeia de chamadas associada ao contexto corrente (e.g.
   * definido pelo {@link Current}). A cadeia de chamadas informada foi
   * associada previamente pela opera��o
   * {@link #joinChain(CallerChain) joinChain}. Caso o contexto corrente n�o
   * tenha nenhuma cadeia associada, essa opera��o retornar� {@code null}.
   *
   * @return A cadeia de chamadas associada ao contexto corrente ou {@code
   * null} .
   */
  CallerChain joinedChain();

  /**
   * Codifica uma cadeia de chamadas para permitir a persist�ncia ou
   * transfer�ncia da informa��o.
   *
   * @param chain A cadeia a ser codificada.
   * @return A cadeia codificada.
   */
  byte[] encodeChain(CallerChain chain);

  /**
   * Decodifica uma cadeia para o formato {@link CallerChain}.
   *
   * @param encoded Os bytes que representam a cadeia.
   * @return A cadeia de chamadas no formato {@link CallerChain}.
   * @throws InvalidEncodedStream Caso a cadeia n�o esteja no formato esperado.
   */
  CallerChain decodeChain(byte[] encoded) throws InvalidEncodedStream;

  /**
   * Codifica um segredo de autentica��o compartilhada para permitir a
   * persist�ncia ou transfer�ncia da informa��o.
   *
   * @param secret O segredo de autentica��o compartilhada a ser codificado.
   * @return O segredo codificado.
   */
  byte[] encodeSharedAuth(SharedAuthSecret secret);

  /**
   * Decodifica um segredo de autentica��o compartilhada para o formato
   * ({@link SharedAuthSecret}).
   *
   * @param encoded Os bytes que representam o segredo.
   * @return O segredo de autentica��o compartilhada decodificado.
   * @throws InvalidEncodedStream Caso o segredo n�o esteja no formato
   * esperado.
   */
  SharedAuthSecret decodeSharedAuth(byte[] encoded) throws InvalidEncodedStream;
}
