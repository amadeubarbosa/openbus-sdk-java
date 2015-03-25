package tecgraf.openbus;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.Current;

import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidToken;
import tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.InvalidEncodedStream;
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
   * Cria uma conexão para um barramento indicado por um endereço de rede.
   * <p>
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
  Connection connectByAddress(String host, int port);

  /**
   * Cria uma conexão para um barramento indicado por um endereço de rede.
   * <p>
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
   *        </ul>
   * 
   * @return Conexão criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade não é válido.
   */
  Connection connectByAddress(String host, int port, Properties props)
    throws InvalidPropertyValue;

  /**
   * Cria uma conexão para um barramento indicado por um endereço de rede.
   * <p>
   * Cria uma conexão para um barramento. O barramento é indicado por uma
   * referência CORBA a um componente SCS que representa os serviços núcleo do
   * barramento. Essa função deve ser utilizada ao invés da
   * {@link OpenBusContext#connectByAddress} para permitir o uso de SSL nas
   * comunicações com o núcleo do barramento.
   * 
   * @param reference Referência CORBA a um componente SCS que representa os
   *        serviços núcleo do barramento.
   * 
   * @return Conexão criada.
   */
  Connection connectByReference(org.omg.CORBA.Object reference);

  /**
   * Cria uma conexão para um barramento indicado por um endereço de rede.
   * <p>
   * Cria uma conexão para um barramento. O barramento é indicado por uma
   * referência CORBA a um componente SCS que representa os serviços núcleo do
   * barramento. Essa função deve ser utilizada ao invés da
   * {@link OpenBusContext#connectByAddress} para permitir o uso de SSL nas
   * comunicações com o núcleo do barramento.
   * 
   * @param reference Referência CORBA a um componente SCS que representa os
   *        serviços núcleo do barramento.
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
   *        </ul>
   * 
   * @return Conexão criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade não é válido.
   */
  Connection connectByReference(org.omg.CORBA.Object reference, Properties props)
    throws InvalidPropertyValue;

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
  @Deprecated
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
   *        </ul>
   * 
   * @return Conexão criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade não é válido.
   */
  @Deprecated
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
   * Cria uma cadeia de chamadas para a entidade para a entidade especificada.
   * <p>
   * Cria uma nova cadeia de chamadas para a entidade especificada, onde o dono
   * da cadeia é a conexão corrente ({@link #getCurrentConnection()}) e
   * utiliza-se a cadeia atual ({@link #getJoinedChain()}) como a cadeia que se
   * deseja dar seguimento ao encadeamento. O identificador de login
   * especificado deve ser um login atualmente válido para que a operação tenha
   * sucesso.
   * 
   * @param entity entidade para a qual deseja-se enviar a cadeia.
   * @return a cadeia gerada para ser utilizada pela entidade com o login
   *         especificado.
   * 
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do barramento
   *         que impediu a criação da cadeia.
   */
  CallerChain makeChainFor(String entity) throws ServiceFailure;

  /**
   * Cria uma cadeia de chamadas assinada pelo barramento com informações de uma
   * autenticação externa ao barramento.
   * <p>
   * A cadeia criada pode somente ser utilizada pela entidade do login que faz a
   * chamada. O conteúdo da cadeia é dado pelas informações obtidas através do
   * token indicado.
   * 
   * @param token Valor opaco que representa uma informação de autenticação
   *        externa.
   * @param domain Identificador do domínio de autenticação.
   * @return A nova cadeia de chamadas assinada.
   *
   * @exception InvalidToken O token fornecido não foi reconhecido.
   * @exception UnknownDomain O domínio de autenticação não é conhecido.
   * @exception WrongEncoding A importação falhou, pois o token não foi
   *            codificado corretamente com a chave pública do barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a criação da cadeia.
   */
  CallerChain importChain(byte[] token, String domain) throws InvalidToken,
    UnknownDomain, ServiceFailure, WrongEncoding;

  /**
   * Codifica uma cadeia de chamadas ({@link CallerChain}) para um stream de
   * bytes.
   * <p>
   * Codifica uma cadeia de chamadas em um stream de bytes para permitir a
   * persistência ou transferência da informação.
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
   * {@link CallerChain}.
   * 
   * @param encoded o stream de bytes que representam a cadeia
   * @return a cadeia de chamadas no formato {@link CallerChain}.
   * @throws InvalidEncodedStream Caso a stream de bytes não seja do formato
   *         esperado.
   */
  CallerChain decodeChain(byte[] encoded) throws InvalidEncodedStream;

  /**
   * \brief Codifica um segredo de autenticação compartilhada (
   * {@link SharedAuthSecret}) para um stream de bytes.
   * 
   * Codifica um segredo de autenticação compartilhada em um stream de bytes
   * para permitir a persistência ou transferência da informação.
   * 
   * @param secret Segredo de autenticação compartilhada a ser codificado.
   * @return Cadeia codificada em um stream de bytes.
   */
  byte[] encodeSharedAuth(SharedAuthSecret secret);

  /**
   * Decodifica um segredo de autenticação compartilhada (
   * {@link SharedAuthSecret}) a partir de um stream de bytes.
   * <p>
   * Decodifica um segredo de autenticação compartilhada a partir de um stream
   * de bytes.
   * 
   * @param encoded Stream de bytes contendo a codificação do segredo.
   * @return Segredo de autenticação compartilhada decodificado.
   * @throws InvalidEncodedStream Caso a stream de bytes não seja do formato
   *         esperado.
   */
  SharedAuthSecret decodeSharedAuth(byte[] encoded) throws InvalidEncodedStream;

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
