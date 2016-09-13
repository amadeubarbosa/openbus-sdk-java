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
 * informações que identificam essas chamadas em barramentos OpenBus.
 * <p>
 * O contexto de uma chamada pode ser definido pela linha de execução atual do
 * programa em que executa uma chamada, o que pode ser a <i>thread</i> em
 * execução ou mais comumente o {@link Current} do padrão CORBA. As
 * informações acessíveis através do {@link OpenBusContext} se referem
 * basicamente à identificação da origem das chamadas, ou seja, o nome das
 * entidades que autenticaram os acessos ao barramento que originaram as
 * chamadas.
 * <p>
 * A identificação de chamadas no barramento é controlada no OpenBusContext
 * através da manipulação de duas abstrações representadas pelas seguintes
 * interfaces:
 * <ul>
 * <li> {@link Connection}: Representa um acesso ao barramento, que é usado
 * tanto para fazer chamadas como para receber chamadas através do barramento
 * . Para tal a conexão precisa estar autenticada, ou seja, ter um login. Cada
 * chamada feita através do ORB é enviada com as informações do login da conexão
 * associada ao contexto em que a chamada foi realizada. Cada chamada recebida
 * também deve vir através de uma conexão com login, que deve ser o mesmo login
 * com que chamadas aninhadas a essa chamada original devem ser feitas.
 * <li> {@link CallChain}: Representa a identificação de todos os acessos ao
 * barramento que originaram uma chamada recebida. Sempre que uma chamada é
 * recebida e executada, é possível obter uma cadeia de chamadas através da
 * qual é possível inspecionar as informações de acesso que originaram a chamada
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
   * utiliza o RootPOA, mas é possível alterar o POA através do método
   * {@link #POA(POA)}.
   *
   * @return O POA a ser utilizado ao criar conexões.
   */
  POA POA();

  /**
   * Configura o POA que o OpenBusContext utilizará ao criar conexões.
   *
   * @param poa O POA a ser utilizado.
   */
  void POA(POA poa);

  /**
   * <i>Callback</i> a ser chamada para determinar a conexão a ser utilizada
   * para receber cada chamada.
   * <p>
   * Essa callback deve fornecer a conexão a ser utilizada para para receber
   * uma chamada. Essa conexão será a única conexão através da qual novas
   * chamadas aninhadas à chamada recebida poderão ser feitas (veja a
   * operação {@link #joinChain}).
   * <p>
   * Se a <i>callback</i> estiver definida como {@code null} e houver uma
   * conexão padrão definida, a conexão padrão será utilizada para receber a
   * chamada. Caso ambos sejam {@code null}, a chamada não poderá ser
   * atendida e retornará um erro para o cliente remoto.
   *
   * @param callback Objeto que implementa a interface de <i>callback</i> ou
   * {@code null} para remover o objeto atual.
   */
  void onCallDispatch(CallDispatchCallback callback);

  /**
   * Fornece a callback configurada para receber chamadas.
   * 
   * @return A callback ou {@code null} caso ela não exista.
   */
  CallDispatchCallback onCallDispatch();

  /**
   * Cria uma conexão para um barramento. O barramento é definido por um nome ou
   * endereço de rede e um número de porta, onde os serviços núcleo daquele
   * barramento estão executando.
   *
   * Esta chamada não implica em chamadas remotas, apenas realiza a
   * configuração necessária para acessar um barramento e realizar
   * autenticações.
   * 
   * @param host Endereço ou nome de rede onde os serviços núcleo do barramento
   *        estão executando.
   * @param port Porta onde os serviços núcleo do barramento estão executando.
   * 
   * @return A conexão criada.
   */
  Connection connectByAddress(String host, int port);

  /**
   * Cria uma conexão para um barramento. O barramento é definido por um nome ou
   * endereço de rede e um número de porta, onde os serviços núcleo daquele
   * barramento estão executando.
   *
   * Esta chamada não implica em chamadas remotas, apenas realiza a
   * configuração necessária para acessar um barramento e realizar
   * autenticações.
   *
   * @param host Endereço ou nome de rede onde os serviços núcleo do barramento
   *        estão executando.
   * @param port Porta onde os serviços núcleo do barramento estão executando.
   * @param props Lista opcional de propriedades que definem algumas
   *        configurações sobre a forma que as chamadas realizadas ou validadas
   *        com essa conexão são feitas. As propriedades válidas estão
   *        definidas em {@link tecgraf.openbus.core.OpenBusProperty}.
   * 
   * @return A conexão criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade não é válido.
   */
  Connection connectByAddress(String host, int port, Properties props)
    throws InvalidPropertyValue;

  /**
   * Cria uma conexão para um barramento. O barramento é definido por uma
   * referência CORBA a um componente SCS que representa os serviços núcleo do
   * barramento. Essa função deve ser utilizada ao invés da
   * {@link #connectByAddress} para permitir o uso de SSL nas
   * comunicações com o núcleo do barramento.
   *
   * Esta chamada não implica em chamadas remotas, apenas realiza a
   * configuração necessária para acessar um barramento e realizar
   * autenticações.
   *
   * @param reference Referência CORBA a um componente SCS que representa os
   *        serviços núcleo do barramento.
   * 
   * @return A conexão criada.
   */
  Connection connectByReference(org.omg.CORBA.Object reference);

  /**
   * Cria uma conexão para um barramento. O barramento é definido por uma
   * referência CORBA a um componente SCS que representa os serviços núcleo do
   * barramento. Essa função deve ser utilizada ao invés da
   * {@link #connectByAddress} para permitir o uso de SSL nas
   * comunicações com o núcleo do barramento.
   *
   * Esta chamada não implica em chamadas remotas, apenas realiza a
   * configuração necessária para acessar um barramento e realizar
   * autenticações.
   *
   * @param reference Referência CORBA a um componente SCS que representa os
   *        serviços núcleo do barramento.
   * @param props Lista opcional de propriedades que definem algumas
   *        configurações sobre a forma que as chamadas realizadas ou validadas
   *        com essa conexão são feitas. As propriedades válidas estão
   *        definidas em {@link tecgraf.openbus.core.OpenBusProperty}.
   * 
   * @return A conexão criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade não é válido.
   */
  Connection connectByReference(org.omg.CORBA.Object reference, Properties props)
    throws InvalidPropertyValue;

  /**
   * Cria uma conexão para um barramento. O barramento é definido por um nome ou
   * endereço de rede e um número de porta, onde os serviços núcleo daquele
   * barramento estão executando.
   *
   * Esta chamada não implica em chamadas remotas, apenas realiza a
   * configuração necessária para acessar um barramento e realizar
   * autenticações.
   *
   * @param host Endereço ou nome de rede onde os serviços núcleo do barramento
   *        estão executando.
   * @param port Porta onde os serviços núcleo do barramento estão executando.
   * 
   * @return A conexão criada.
   */
  @Deprecated
  Connection createConnection(String host, int port);

  /**
   * Cria uma conexão para um barramento. O barramento é definido por um nome ou
   * endereço de rede e um número de porta, onde os serviços núcleo daquele
   * barramento estão executando.
   *
   * Esta chamada não implica em chamadas remotas, apenas realiza a
   * configuração necessária para acessar um barramento e realizar
   * autenticações.
   *
   * @param host Endereço ou nome de rede onde os serviços núcleo do barramento
   *        estão executando.
   * @param port Porta onde os serviços núcleo do barramento estão executando.
   * @param props Lista opcional de propriedades que definem algumas
   *        configurações sobre a forma que as chamadas realizadas ou validadas
   *        com essa conexão são feitas. As propriedades válidas estão
   *        definidas em {@link tecgraf.openbus.core.OpenBusProperty}.
   * 
   * @return A conexão criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade não é válido.
   */
  @Deprecated
  Connection createConnection(String host, int port, Properties props)
    throws InvalidPropertyValue;

  /**
   * Define a conexão padrão a ser utilizada em chamadas sempre que não houver
   * uma conexão específica definida no contexto atual, como é feito através das
   * operações {@link #currentConnection(Connection)} e
   * {@link #onCallDispatch(CallDispatchCallback)}.
   *
   * A conexão padrão não afeta chamadas feitas por recursos de uma conexão.
   * 
   * @param connection A conexão a ser definida como conexão padrão.
   * 
   * @return A conexão definida como conexão padrão anteriormente, ou
   *         {@code null} se não havia conexão padrão definida.
   */
  Connection defaultConnection(Connection connection);

  /**
   * Fornece a conexão padrão a ser usada nas chamadas. Veja operação
   * {@link #defaultConnection defaultConnection}.
   *
   * A conexão padrão não afeta chamadas feitas por recursos de uma conexão.
   *
   * @return A conexão definida como conexão padrão.
   */
  Connection defaultConnection();

  /**
   * Define a conexão a ser utilizada em todas as chamadas feitas no contexto
   * atual.
   *
   * Se a conexão corrente estiver definida como {@code null} e houver uma
   * conexão padrão definida, a conexão padrão será utilizada para realizar
   * chamadas. Caso ambas sejam {@code null}, chamadas não poderão ser
   * realizadas e um erro será lançado para a aplicação.
   *
   * A conexão corrente não afeta chamadas feitas por recursos de uma conexão.
   *
   * @param connection A conexão a ser associada ao contexto corrente.
   * 
   * @return A conexão definida como a conexão corrente anteriormente, ou
   * {@code null} se não havia conexão definida.
   */
  Connection currentConnection(Connection connection);

  /**
   * Fornece a conexão associada ao contexto corrente e que será utilizada em
   * chamadas. Ela pode ter sido definida pela operação
   * {@link #currentConnection(Connection)} ou
   * {@link #defaultConnection(Connection)}.
   *
   * A conexão padrão não afeta chamadas feitas por recursos de uma conexão.
   *
   * @return A conexão associada ao contexto corrente, ou {@code null} caso
   *         não haja nenhuma conexão associada.
   */
  Connection currentConnection();

  /**
   * Fornece a cadeia de chamadas à qual a execução corrente pertence.
   * <p>
   * Essa operação devolve um objeto que representa a cadeia de chamadas do
   * barramento que esta chamada faz parte.
   *
   * @return A cadeia da chamada em execução.
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
   * Remove a associação da cadeia de chamadas ao contexto corrente (e.g.
   * definido pelo {@link Current}), fazendo com que todas as chamadas seguintes
   * feitas neste mesmo contexto deixem de fazer parte da cadeia de chamadas
   * associada previamente. Ou seja, todas as chamadas passam a iniciar novas
   * cadeias de chamada.
   */
  void exitChain();

  /**
   * Fornece a cadeia de chamadas associada ao contexto corrente (e.g.
   * definido pelo {@link Current}). A cadeia de chamadas informada foi
   * associada previamente pela operação
   * {@link #joinChain(CallerChain) joinChain}. Caso o contexto corrente não
   * tenha nenhuma cadeia associada, essa operação retornará {@code null}.
   *
   * @return A cadeia de chamadas associada ao contexto corrente ou {@code
   * null} .
   */
  CallerChain joinedChain();

  /**
   * Codifica uma cadeia de chamadas para permitir a persistência ou
   * transferência da informação.
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
   * @throws InvalidEncodedStream Caso a cadeia não esteja no formato esperado.
   */
  CallerChain decodeChain(byte[] encoded) throws InvalidEncodedStream;

  /**
   * Codifica um segredo de autenticação compartilhada para permitir a
   * persistência ou transferência da informação.
   *
   * @param secret O segredo de autenticação compartilhada a ser codificado.
   * @return O segredo codificado.
   */
  byte[] encodeSharedAuth(SharedAuthSecret secret);

  /**
   * Decodifica um segredo de autenticação compartilhada para o formato
   * ({@link SharedAuthSecret}).
   *
   * @param encoded Os bytes que representam o segredo.
   * @return O segredo de autenticação compartilhada decodificado.
   * @throws InvalidEncodedStream Caso o segredo não esteja no formato
   * esperado.
   */
  SharedAuthSecret decodeSharedAuth(byte[] encoded) throws InvalidEncodedStream;
}
