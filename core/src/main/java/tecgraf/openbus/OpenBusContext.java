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
 * informa��es que identificam essas chamadas em barramentos OpenBus.
 * <p>
 * O contexto de uma chamada pode ser definido pela linha de execu��o atual do
 * programa em que executa uma chamada, o que pode ser a thread em execu��o ou
 * mais comumente o {@link Current} do padr�o CORBA. As informa��es acess�veis
 * atrav�s do {@link OpenBusContext} se referem basicamente � identifica��o da
 * origem das chamadas, ou seja, nome das entidades que autenticaram os acessos
 * ao barramento que originaram as chamadas.
 * <p>
 * A identifca��o de chamadas no barramento � controlada atrav�s do
 * OpenBusContext atrav�s da manipula��o de duas abstra��es representadas pelas
 * seguintes interfaces:
 * <ul>
 * <li> {@link Connection}: Representa um acesso ao barramento, que � usado tanto
 * para fazer chamadas como para receber chamadas atrav�s do barramento. Para
 * tanto a conex�o precisa estar autenticada, ou seja, logada. Cada chamada
 * feita atrav�s do ORB � enviada com as informa��es do login da conex�o
 * associada ao contexto em que a chamada foi realizada. Cada chamada recebida
 * tamb�m deve vir atrav�s de uma conex�o logada, que deve ser o mesmo login com
 * que chamadas aninhadas a essa chamada original devem ser feitas.
 * <li> {@link CallChain}: Representa a identica��o de todos os acessos ao
 * barramento que originaram uma chamada recebida. Sempre que uma chamada �
 * recebida e executada, � poss�vel obter um CallChain atrav�s do qual �
 * poss�vel inspecionar as informa��es de acesso que originaram a chamada
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
   * Callback a ser chamada para determinar a conex�o a ser utilizada para
   * receber cada chamada.
   * <p>
   * Esse atributo � utilizado para definir um objeto que implementa uma
   * interface de callback a ser chamada sempre que a conex�o receber uma do
   * barramento. Essa callback deve devolver a conex�o a ser utilizada para para
   * receber a chamada. A conex�o utilizada para receber a chamada ser� a �nica
   * conex�o atrav�s do qual novas chamadas aninhadas � chamada recebida poder�o
   * ser feitas (veja a opera��o {@link OpenBusContext#joinChain}).
   * <p>
   * Se o objeto de callback for definido como <code>null</code> ou devolver
   * <code>null</code>, a conex�o padr�o � utilizada para receber achamada, caso
   * esta esteja definida.
   * <p>
   * Caso esse atributo seja <code>null</code>, nenhum objeto de callback �
   * chamado na ocorr�ncia desse evento e ???
   * 
   * @param callback Objeto que implementa a interface de callback a ser chamada
   *        ou <code>null</code> caso nenhum objeto deva ser chamado na
   *        ocorr�ncia desse evento.
   */
  void onCallDispatch(CallDispatchCallback callback);

  /**
   * Recupera a callback a ser chamada sempre que a conex�o receber uma do
   * barramento.
   * 
   * @return a callback ou <code>null</code> caso ela n�o exista.
   */
  CallDispatchCallback onCallDispatch();

  /**
   * Cria uma conex�o para um barramento indicado por um endere�o de rede.
   * <p>
   * Cria uma conex�o para um barramento. O barramento � indicado por um nome ou
   * endere�o de rede e um n�mero de porta, onde os servi�os n�cleo daquele
   * barramento est�o executando.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * 
   * @return Conex�o criada.
   */
  Connection connectByAddress(String host, int port);

  /**
   * Cria uma conex�o para um barramento indicado por um endere�o de rede.
   * <p>
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
   *        </ul>
   * 
   * @return Conex�o criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade n�o � v�lido.
   */
  Connection connectByAddress(String host, int port, Properties props)
    throws InvalidPropertyValue;

  /**
   * Cria uma conex�o para um barramento indicado por um endere�o de rede.
   * <p>
   * Cria uma conex�o para um barramento. O barramento � indicado por uma
   * refer�ncia CORBA a um componente SCS que representa os servi�os n�cleo do
   * barramento. Essa fun��o deve ser utilizada ao inv�s da
   * {@link OpenBusContext#connectByAddress} para permitir o uso de SSL nas
   * comunica��es com o n�cleo do barramento.
   * 
   * @param reference Refer�ncia CORBA a um componente SCS que representa os
   *        servi�os n�cleo do barramento.
   * 
   * @return Conex�o criada.
   */
  Connection connectByReference(org.omg.CORBA.Object reference);

  /**
   * Cria uma conex�o para um barramento indicado por um endere�o de rede.
   * <p>
   * Cria uma conex�o para um barramento. O barramento � indicado por uma
   * refer�ncia CORBA a um componente SCS que representa os servi�os n�cleo do
   * barramento. Essa fun��o deve ser utilizada ao inv�s da
   * {@link OpenBusContext#connectByAddress} para permitir o uso de SSL nas
   * comunica��es com o n�cleo do barramento.
   * 
   * @param reference Refer�ncia CORBA a um componente SCS que representa os
   *        servi�os n�cleo do barramento.
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
   *        </ul>
   * 
   * @return Conex�o criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade n�o � v�lido.
   */
  Connection connectByReference(org.omg.CORBA.Object reference, Properties props)
    throws InvalidPropertyValue;

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
   */
  @Deprecated
  Connection createConnection(String host, int port);

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
   *        </ul>
   * 
   * @return Conex�o criada.
   * 
   * @throws InvalidPropertyValue O valor de uma propriedade n�o � v�lido.
   */
  @Deprecated
  Connection createConnection(String host, int port, Properties props)
    throws InvalidPropertyValue;

  /**
   * Define a conex�o padr�o a ser usada nas chamadas.
   * <p>
   * Define uma conex�o a ser utilizada em chamadas sempre que n�o houver uma
   * conex�o espec�fica definida no contexto atual, como � feito atrav�s da
   * opera��o {@link OpenBusContext#setCurrentConnection(Connection)
   * setRequester}. Quando <code>conn</code> � <code>null</code> nenhuma conex�o
   * fica definida como a conex�o padr�o.
   * 
   * @param conn Conex�o a ser definida como conex�o padr�o.
   * 
   * @return Conex�o definida como conex�o padr�o anteriormente, ou
   *         <code>null</code> se n�o havia conex�o padr�o definida
   *         anteriormente.
   */
  Connection setDefaultConnection(Connection conn);

  /**
   * Devolve a conex�o padr�o.
   * <p>
   * Veja opera��o {@link OpenBusContext#setDefaultConnection
   * setDefaultConnection}.
   * 
   * @return Conex�o definida como conex�o padr�o.
   */
  Connection getDefaultConnection();

  /**
   * Define a conex�o associada ao contexto corrente.
   * <p>
   * Define a conex�o a ser utilizada em todas as chamadas feitas no contexto
   * atual. Quando <code>conn</code> � <code>null</code> o contexto passa a
   * ficar sem nenhuma conex�o associada.
   * 
   * @param conn Conex�o a ser associada ao contexto corrente.
   * 
   * @return Conex�o definida como a conex�o corrente anteriormente, ou null se
   *         n�o havia conex�o definida ateriormente.
   */
  Connection setCurrentConnection(Connection conn);

  /**
   * Devolve a conex�o associada ao contexto corrente.
   * <p>
   * Devolve a conex�o associada ao contexto corrente, que pode ter sido
   * definida usando a opera��o {@link OpenBusContext#setCurrentConnection} ou
   * {@link OpenBusContext#setDefaultConnection}.
   * 
   * @return Conex�o associada ao contexto corrente, ou <code>null</code> caso
   *         n�o haja nenhuma conex�o associada.
   */
  Connection getCurrentConnection();

  /**
   * Devolve a cadeia de chamadas � qual a execu��o corrente pertence.
   * <p>
   * Caso a contexto corrente (e.g. definido pelo {@link Current}) seja o
   * contexto de execu��o de uma chamada remota oriunda do barramento dessa
   * conex�o, essa opera��o devolve um objeto que representa a cadeia de
   * chamadas do barramento que esta chamada faz parte. Caso contr�rio, devolve
   * <code>null</code>.
   * 
   * @return Cadeia da chamada em execu��o.
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
   * Remove a associa��o da cadeia de chamadas ao contexto corrente (e.g.
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
   * corrente (e.g. definido pelo {@link Current}) nesta conex�o. A cadeia de
   * chamadas informada foi associada previamente pela opera��o
   * {@link #joinChain(CallerChain) joinChain}. Caso o contexto corrente n�o
   * tenha nenhuma cadeia associada, essa opera��o devolve <code>null</code>.
   * 
   * @return Cadeia de chamadas associada ao contexto corrente ou
   *         <code>null</code> .
   */
  CallerChain getJoinedChain();

  /**
   * Cria uma cadeia de chamadas para a entidade para a entidade especificada.
   * <p>
   * Cria uma nova cadeia de chamadas para a entidade especificada, onde o dono
   * da cadeia � a conex�o corrente ({@link #getCurrentConnection()}) e
   * utiliza-se a cadeia atual ({@link #getJoinedChain()}) como a cadeia que se
   * deseja dar seguimento ao encadeamento. O identificador de login
   * especificado deve ser um login atualmente v�lido para que a opera��o tenha
   * sucesso.
   * 
   * @param entity entidade para a qual deseja-se enviar a cadeia.
   * @return a cadeia gerada para ser utilizada pela entidade com o login
   *         especificado.
   * 
   * @throws ServiceFailure Ocorreu uma falha interna nos servi�os do barramento
   *         que impediu a cria��o da cadeia.
   */
  CallerChain makeChainFor(String entity) throws ServiceFailure;

  /**
   * Cria uma cadeia de chamadas assinada pelo barramento com informa��es de uma
   * autentica��o externa ao barramento.
   * <p>
   * A cadeia criada pode somente ser utilizada pela entidade do login que faz a
   * chamada. O conte�do da cadeia � dado pelas informa��es obtidas atrav�s do
   * token indicado.
   * 
   * @param token Valor opaco que representa uma informa��o de autentica��o
   *        externa.
   * @param domain Identificador do dom�nio de autentica��o.
   * @return A nova cadeia de chamadas assinada.
   *
   * @exception InvalidToken O token fornecido n�o foi reconhecido.
   * @exception UnknownDomain O dom�nio de autentica��o n�o � conhecido.
   * @exception WrongEncoding A importa��o falhou, pois o token n�o foi
   *            codificado corretamente com a chave p�blica do barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a cria��o da cadeia.
   */
  CallerChain importChain(byte[] token, String domain) throws InvalidToken,
    UnknownDomain, ServiceFailure, WrongEncoding;

  /**
   * Codifica uma cadeia de chamadas ({@link CallerChain}) para um stream de
   * bytes.
   * <p>
   * Codifica uma cadeia de chamadas em um stream de bytes para permitir a
   * persist�ncia ou transfer�ncia da informa��o.
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
   * @throws InvalidEncodedStream Caso a stream de bytes n�o seja do formato
   *         esperado.
   */
  CallerChain decodeChain(byte[] encoded) throws InvalidEncodedStream;

  /**
   * \brief Codifica um segredo de autentica��o compartilhada (
   * {@link SharedAuthSecret}) para um stream de bytes.
   * 
   * Codifica um segredo de autentica��o compartilhada em um stream de bytes
   * para permitir a persist�ncia ou transfer�ncia da informa��o.
   * 
   * @param secret Segredo de autentica��o compartilhada a ser codificado.
   * @return Cadeia codificada em um stream de bytes.
   */
  byte[] encodeSharedAuth(SharedAuthSecret secret);

  /**
   * Decodifica um segredo de autentica��o compartilhada (
   * {@link SharedAuthSecret}) a partir de um stream de bytes.
   * <p>
   * Decodifica um segredo de autentica��o compartilhada a partir de um stream
   * de bytes.
   * 
   * @param encoded Stream de bytes contendo a codifica��o do segredo.
   * @return Segredo de autentica��o compartilhada decodificado.
   * @throws InvalidEncodedStream Caso a stream de bytes n�o seja do formato
   *         esperado.
   */
  SharedAuthSecret decodeSharedAuth(byte[] encoded) throws InvalidEncodedStream;

  /**
   * Refer�ncia ao servi�o n�cleo de registro de logins do barramento
   * referenciado no contexto atual.
   * 
   * @return o servi�o de registro de logins.
   */
  LoginRegistry getLoginRegistry();

  /**
   * Refer�ncia ao servi�o n�cleo de registro de ofertas do barramento
   * referenciado no contexto atual.
   * 
   * @return o servi�o de registro de ofertas.
   */
  OfferRegistry getOfferRegistry();
}
