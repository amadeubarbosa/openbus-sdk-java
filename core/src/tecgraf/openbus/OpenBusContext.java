package tecgraf.openbus;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.Current;

import tecgraf.openbus.core.v2_0.credential.CredentialContextId;
import tecgraf.openbus.core.v2_0.data_export.ExportedCallChain;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_0.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.InvalidChainStream;
import tecgraf.openbus.exception.InvalidPropertyValue;

/**
 * Permite controlar o contexto das chamadas de um {@link ORB} para acessar
 * informações que identificam essas chamadas em barramentos OpenBus.
 * <p>
 * O contexto de uma chamada pode ser definido pela linha de execução atual do
 * programa em que executa uma chamada, o que pode ser a thread em execução ou
 * mais comumente o {@link Current} do padrão CORBA. As informações acessíveis
 * através do {@link OpenBusContext} se referem basicamente à identificação da
 * origem das chamadas, ou seja, nome das entidades que autenticaram os acessos
 * ao barramento que originaram as chamadas.
 * <p>
 * A identifcação de chamadas no barramento é controlada através do
 * OpenBusContext através da manipulação de duas abstrações representadas pelas
 * seguintes interfaces:
 * <ul>
 * <li> {@link Connection}: Representa um acesso ao barramento, que é usado tanto
 * para fazer chamadas como para receber chamadas através do barramento. Para
 * tanto a conexão precisa estar autenticada, ou seja, logada. Cada chamada
 * feita através do ORB é enviada com as informações do login da conexão
 * associada ao contexto em que a chamada foi realizada. Cada chamada recebida
 * também deve vir através de uma conexão logada, que deve ser o mesmo login com
 * que chamadas aninhadas a essa chamada original devem ser feitas.
 * <li> {@link CallChain}: Representa a identicação de todos os acessos ao
 * barramento que originaram uma chamada recebida. Sempre que uma chamada é
 * recebida e executada, é possível obter um CallChain através do qual é
 * possível inspecionar as informações de acesso que originaram a chamada
 * recebida.
 * </ul>
 * 
 * @author Tecgraf
 */
public interface OpenBusContext {

  /**
   * Recupera o ORB associado ao ConnectionManager.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Callback a ser chamada para determinar a conexão a ser utilizada para
   * receber cada chamada.
   * <p>
   * Esse atributo é utilizado para definir um objeto que implementa uma
   * interface de callback a ser chamada sempre que a conexão receber uma do
   * barramento. Essa callback deve devolver a conexão a ser utilizada para para
   * receber a chamada. A conexão utilizada para receber a chamada será a única
   * conexão através do qual novas chamadas aninhadas à chamada recebida poderão
   * ser feitas (veja a operação {@link OpenBusContext#joinChain}).
   * <p>
   * Se o objeto de callback for definido como <code>null</code> ou devolver
   * <code>null</code>, a conexão padrão é utilizada para receber achamada, caso
   * esta esteja definida.
   * <p>
   * Caso esse atributo seja <code>null</code>, nenhum objeto de callback é
   * chamado na ocorrência desse evento e ???
   * 
   * @param callback Objeto que implementa a interface de callback a ser chamada
   *        ou <code>null</code> caso nenhum objeto deva ser chamado na
   *        ocorrência desse evento.
   */
  void onCallDispatch(CallDispatchCallback callback);

  /**
   * Recupera a callback a ser chamada sempre que a conexão receber uma do
   * barramento.
   * 
   * @return a callback ou <code>null</code> caso ela não exista.
   */
  CallDispatchCallback onCallDispatch();

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
   */
  Connection createConnection(String host, int port);

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
   * @throws InvalidPropertyValue O valor de uma propriedade não é válido.
   */
  Connection createConnection(String host, int port, Properties props)
    throws InvalidPropertyValue;

  /**
   * Define a conexão padrão a ser usada nas chamadas.
   * <p>
   * Define uma conexão a ser utilizada em chamadas sempre que não houver uma
   * conexão específica definida no contexto atual, como é feito através da
   * operação {@link OpenBusContext#setCurrentConnection(Connection)
   * setRequester}. Quando <code>conn</code> é <code>null</code> nenhuma conexão
   * fica definida como a conexão padrão.
   * 
   * @param conn Conexão a ser definida como conexão padrão.
   * 
   * @return Conexão definida como conexão padrão anteriormente, ou
   *         <code>null</code> se não havia conexão padrão definida
   *         anteriormente.
   */
  Connection setDefaultConnection(Connection conn);

  /**
   * Devolve a conexão padrão.
   * <p>
   * Veja operação {@link OpenBusContext#setDefaultConnection
   * setDefaultConnection}.
   * 
   * @return Conexão definida como conexão padrão.
   */
  Connection getDefaultConnection();

  /**
   * Define a conexão associada ao contexto corrente.
   * <p>
   * Define a conexão a ser utilizada em todas as chamadas feitas no contexto
   * atual. Quando <code>conn</code> é <code>null</code> o contexto passa a
   * ficar sem nenhuma conexão associada.
   * 
   * @param conn Conexão a ser associada ao contexto corrente.
   * 
   * @return Conexão definida como a conexão corrente anteriormente, ou null se
   *         não havia conexão definida ateriormente.
   */
  Connection setCurrentConnection(Connection conn);

  /**
   * Devolve a conexão associada ao contexto corrente.
   * <p>
   * Devolve a conexão associada ao contexto corrente, que pode ter sido
   * definida usando a operação {@link OpenBusContext#setCurrentConnection} ou
   * {@link OpenBusContext#setDefaultConnection}.
   * 
   * @return Conexão associada ao contexto corrente, ou <code>null</code> caso
   *         não haja nenhuma conexão associada.
   */
  Connection getCurrentConnection();

  /**
   * Devolve a cadeia de chamadas à qual a execução corrente pertence.
   * <p>
   * Caso a contexto corrente (e.g. definido pelo {@link Current}) seja o
   * contexto de execução de uma chamada remota oriunda do barramento dessa
   * conexão, essa operação devolve um objeto que representa a cadeia de
   * chamadas do barramento que esta chamada faz parte. Caso contrário, devolve
   * <code>null</code>.
   * 
   * @return Cadeia da chamada em execução.
   */
  CallerChain getCallerChain();

  /**
   * Associa uma cadeia de chamadas ao contexto corrente.
   * <p>
   * Associa uma cadeia de chamadas ao contexto corrente (e.g. definido pelo
   * {@link Current}), de forma que todas as chamadas remotas seguintes neste
   * mesmo contexto sejam feitas como parte dessa cadeia de chamadas.
   * 
   * @param chain Cadeia de chamadas a ser associada ao contexto corrente.
   */
  void joinChain(CallerChain chain);

  /**
   * Associa a cadeia de chamadas obtida em {@link #getCallerChain()} ao
   * contexto corrente.
   * <p>
   * Associa a cadeia de chamadas obtida em {@link #getCallerChain()} ao
   * contexto corrente (e.g. definido pelo {@link Current}), de forma que todas
   * as chamadas remotas seguintes neste mesmo contexto sejam feitas como parte
   * dessa cadeia de chamadas.
   */
  void joinChain();

  /**
   * Faz com que nenhuma cadeia de chamadas esteja associada ao contexto
   * corrente.
   * <p>
   * Remove a associação da cadeia de chamadas ao contexto corrente (e.g.
   * definido pelo {@link Current}), fazendo com que todas as chamadas seguintes
   * feitas neste mesmo contexto deixem de fazer parte da cadeia de chamadas
   * associada previamente. Ou seja, todas as chamadas passam a iniciar novas
   * cadeias de chamada.
   */
  void exitChain();

  /**
   * Devolve a cadeia de chamadas associada ao contexto corrente.
   * <p>
   * Devolve um objeto que representa a cadeia de chamadas associada ao contexto
   * corrente (e.g. definido pelo {@link Current}) nesta conexão. A cadeia de
   * chamadas informada foi associada previamente pela operação
   * {@link #joinChain(CallerChain) joinChain}. Caso o contexto corrente não
   * tenha nenhuma cadeia associada, essa operação devolve <code>null</code>.
   * 
   * @return Cadeia de chamadas associada ao contexto corrente ou
   *         <code>null</code> .
   */
  CallerChain getJoinedChain();

  /**
   * Cria uma cadeia de chamadas para a entidade com o identificador de login
   * especificado.
   * <p>
   * Cria uma nova cadeia de chamadas para a entidade especificada, onde o dono
   * da cadeia é a conexão corrente ({@link #getCurrentConnection()}) e
   * utiliza-se a cadeia atual ({@link #getJoinedChain()}) como a cadeia que se
   * deseja dar seguimento ao encadeamento. O identificador de login
   * especificado deve ser um login atualmente válido para que a operação tenha
   * sucesso.
   * 
   * @param loginId identificador de login da entidade para a qual deseja-se
   *        enviar a cadeia.
   * @return a cadeia gerada para ser utilizada pela entidade com o login
   *         especificado.
   * 
   * @throws InvalidLogins Caso o login especificado seja inválido.
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do barramento
   *         que impediu a criação da cadeia.
   */
  CallerChain makeChainFor(String loginId) throws InvalidLogins, ServiceFailure;

  /**
   * Codifica uma cadeia de chamadas ({@link CallerChain}) para um stream de
   * bytes.
   * <p>
   * Codifica uma cadeia de chamadas em um stream de bytes para permitir a
   * persistência ou transferência da informação. A codificação é realizada em
   * CDR e possui um identificador de versão concatenado com as informações da
   * cadeia ({@link CredentialContextId} + {@link ExportedCallChain}). Sendo
   * assim, a stream só será decodificada com sucesso por alguém que entenda
   * esta mesma codificação.
   * 
   * @param chain a cadeia a ser codificada.
   * @return a cadeia codificada em um stream de bytes.
   */
  byte[] encodeChain(CallerChain chain);

  /**
   * Decodifica um stream de bytes de uma cadeia para o formato
   * {@link CallerChain}.
   * <p>
   * Decodifica um stream de bytes de uma cadeia para o formato
   * {@link CallerChain}. Espera-se que a stream de bytes esteja codificada em
   * CDR e seja formada por um identificador de versão concatenado com as
   * informações da cadeia ({@link CredentialContextId} +
   * {@link ExportedCallChain}).
   * 
   * @param encoded o stream de bytes que representam a cadeia
   * @return a cadeia de chamadas no formato {@link CallerChain}.
   * @throws InvalidChainStream Caso a stream de bytes não seja do formato
   *         esperado.
   */
  CallerChain decodeChain(byte[] encoded) throws InvalidChainStream;

  /**
   * Referência ao serviço núcleo de registro de logins do barramento
   * referenciado no contexto atual.
   * 
   * @return o serviço de registro de logins.
   */
  LoginRegistry getLoginRegistry();

  /**
   * Referência ao serviço núcleo de registro de ofertas do barramento
   * referenciado no contexto atual.
   * 
   * @return o serviço de registro de ofertas.
   */
  OfferRegistry getOfferRegistry();
}
