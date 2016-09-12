package tecgraf.openbus;

import java.security.interfaces.RSAPrivateKey;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import org.omg.PortableServer.POA;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidToken;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.WrongBus;

/**
 * Objeto que representa uma forma de acesso a um barramento.
 * <p>
 * Basicamente, uma conex�o � usada para representar uma identidade de acesso
 * a um barramento. � poss�vel uma aplica��o assumir m�ltiplas identidades ao
 * acessar um ou mais barramentos criando m�ltiplas conex�es para esses
 * barramentos.
 * <p>
 * Para que as conex�es possam ser efetivamente utilizadas elas precisam estar
 * autenticadas no barramento, que pode ser visto como um identificador de
 * acesso. Cada <i>login</i> possui um identificador �nico e � autenticado em
 * nome de uma entidade, que pode representar um sistema computacional ou
 * mesmo uma pessoa. A fun��o da entidade � atribuir a responsabilidade �s
 * chamadas feitas com aquele <i>login</i>.
 * <p>
 * � importante notar que a conex�o s� define uma forma de acesso, mas n�o �
 * usada diretamente pela aplica��o ao realizar ou receber chamadas, pois as
 * chamadas ocorrem usando <i>proxies</i> e <i>servants</i> de um {@link ORB}
 * . As conex�es que s�o efetivamente usadas nas chamadas do ORB s�o definidas
 * atrav�s do {@link OpenBusContext} associado ao ORB.
 *
 * Para o caso de recursos mantidos por uma conex�o atrav�s de seus
 * {@link LoginRegistry} e {@link OfferRegistry}, n�o � necess�rio definir a
 * conex�o atrav�s do {@link OpenBusContext}, pois esses recursos j� estar�o
 * atrelados � conex�o a qual pertencem.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Fornece o {@link ORB} correspondente ao {@link OpenBusContext} a partir do
   * qual essa conex�o foi criada.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB ORB();

  /**
   * Fornece o POA associado a essa conex�o.
   *
   * @return o POA
   */
  POA POA();

  /**
   * Fornece o contexto do OpenBus associado a essa conex�o.
   *
   * @return o contexto do OpenBus
   */
  OpenBusContext context();

  /**
   * Fornece o identificador do barramento ao qual essa conex�o se refere.
   * 
   * @return o identificador do barramento.
   */
  String busId();

  /**
   * Fornece as Informa��es do login dessa conex�o ou {@code null} se a
   * conex�o n�o estiver autenticada, ou seja, n�o tiver um login v�lido no
   * barramento.
   * 
   * @return As informa��es do login.
   */
  LoginInfo login();

  /**
   * Efetua login de uma entidade usando autentica��o por senha.
   * <p>
   * A autentica��o por senha � validada atrav�s de um dos validadores de senha
   * definidos pelo administrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autentica��o no barramento da entidade.
   * @param domain Identificador do dom�nio de autentica��o.
   * 
   * @throws AlreadyLoggedIn A conex�o j� est� autenticada.
   * @throws AccessDenied A senha fornecida para autentica��o da entidade n�o
   *            foi validada pelo barramento.
   * @throws TooManyAttempts A autentica��o foi recusada por um n�mero
   *            excessivo de tentativas inv�lidas de autentica��o por senha.
   * @throws UnknownDomain O dom�nio de autentica��o n�o � conhecido.
   * @throws ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   * @throws WrongEncoding A autentica��o falhou, pois a senha n�o foi
   *            codificada corretamente com a chave p�blica do barramento.
   */
  void loginByPassword(String entity, byte[] password, String domain)
    throws AccessDenied, AlreadyLoggedIn, TooManyAttempts, UnknownDomain,
    ServiceFailure, WrongEncoding;

  /**
   * Efetua login de uma entidade usando autentica��o por chave privada.
   * <p>
   * A autentica��o por chave privada � validada atrav�s de um certificado
   * registrado pelo administrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey Chave privada correspondente ao certificado registrado
   *                   no barramento.
   * 
   * @throws AlreadyLoggedIn A conex�o j� est� autenticada.
   * @throws AccessDenied A chave privada fornecida n�o corresponde ao
   *            certificado da entidade registrado no barramento.
   * @throws MissingCertificate N�o h� certificado para essa entidade
   *            registrado no barramento.
   * @throws ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   * @throws WrongEncoding A autentica��o falhou, pois a resposta ao desafio
   *            n�o foi codificada corretamente com a chave p�blica do
   *            barramento.
   */
  void loginByPrivateKey(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedIn, AccessDenied, MissingCertificate, ServiceFailure, WrongEncoding;

  /**
   * Efetua login de uma entidade utilizando uma <i>callback</i> que retorne
   * dados de autentica��o, como autentica��es compartilhadas.
   * <p>
   * No caso espec�fico da autentica��o compartilhada, esta deve ser feita a
   * partir de um segredo obtido atrav�s da opera��o
   * {@link #startSharedAuth() startSharedAuth} de uma conex�o autenticada.
   *
   * @param callback Callback a ser utilizada para obter os dados de
   *                 autentica��o a serem utilizados no login.
   *
   * @throws InvalidLoginProcess A tentativa de login associada ao segredo
   *         informado � inv�lido, por exemplo depois do segredo ser
   *         cancelado, ter expirado, ou j� ter sido utilizado.
   * @throws AlreadyLoggedIn A conex�o j� est� autenticada.
   * @throws WrongBus As informa��es de autentica��o n�o pertencem ao
   *         barramento contactado.
   * @throws AccessDenied As informa��es de autentica��o n�o correspondem
   *         ao esperado pelo barramento.
   * @throws ServiceFailure Ocorreu uma falha interna nos servi�os do
   *         barramento que impediu a autentica��o da conex�o.
   * @throws MissingCertificate O barramento n�o tem um certificado p�blico
   *         para a entidade fornecida. Como as informa��es de login s�o
   *         dadas pela callback, deve-se verificar se a callback est�
   *         obtendo-as corretamente.
   * @throws TooManyAttempts O barramento bloqueou tentativas de login desta
   *         conex�o temporariamente, devido a um excesso de tentativas num
   *         curto per�odo de tempo.
   * @throws UnknownDomain O barramento n�o reconhece o dom�nio especificado
   *         para este login. Como as informa��es de login s�o dadas pela
   *         callback, deve-se verificar se a callback est� obtendo-as
   *         corretamente.
   * @throws WrongEncoding A autentica��o falhou, pois a resposta ao desafio
   *         n�o foi codificada corretamente com a chave p�blica do barramento.
   */
  void loginByCallback(LoginCallback callback) throws AlreadyLoggedIn, WrongBus,
    InvalidLoginProcess, AccessDenied, TooManyAttempts, UnknownDomain,
    MissingCertificate, ServiceFailure, WrongEncoding;

  /**
   * Inicia o processo de login por autentica��o compartilhada.
   *
   * A autentica��o compartilhada permite criar um novo login compartilhando a
   * mesma autentica��o do login atual da conex�o. Portanto, essa opera��o s�
   * pode ser chamada enquanto a conex�o estiver autenticada, caso contr�rio a
   * exce��o de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] ser� lan�ada
   * . As informa��es fornecidas por essa opera��o devem ser passadas para a
   * opera��o {@link #loginByCallback(LoginCallback)} para conclus�o do
   * processo de login por autentica��o compartilhada. Isso deve ser feito
   * dentro do tempo de lease definido pelo administrador do barramento. Caso
   * contr�rio essas informa��es se tornar�o inv�lidas e n�o poder�o mais ser
   * utilizadas para criar um login.
   * 
   * @return Segredo a ser fornecido na conclus�o do processo de login.
   * 
   * @throws ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a obten��o do objeto de login e segredo.
   */
  SharedAuthSecret startSharedAuth() throws ServiceFailure;

  /**
   * Efetua logout da conex�o, tornando o login atual inv�lido.
   * <p>
   * Ap�s a chamada a essa opera��o a conex�o fica desautenticada, implicando
   * numa exce��o de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] para
   * qualquer chamada realizada pelo ORB usando essa conex�o. Chamadas
   * recebidas por esse ORB ser�o respondidas com a exce��o
   * {@link NO_PERMISSION}[{@link UnknownBusCode}] indicando que n�o foi
   * poss�vel validar a chamada devido � conex�o estar desautenticada.
   *
   * Um logout expl�cito incorre na remo��o de todas as ofertas e todos os tipos
   * de observadores mantidos pela biblioteca de acesso OpenBus, tanto
   * localmente como no barramento. Um login posterior n�o levar� ao
   * recadastro autom�tico desses recursos.
   *
   * @return {@code True} se o processo de logout for conclu�do com �xito e
   *         {@code false} se n�o for poss�vel invalidar o login atual. Mesmo
   *         que seja retornado {@code false}, os recursos que eram mantidos
   *         por este login n�o ser�o mais renovados e, ap�s o tempo de
   *         <i>lease</i> do barramento, ser�o removidos automaticamente por
   *         falta de renova��o.
   * 
   * @throws ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento durante chamada ao remota.
   */
  boolean logout() throws ServiceFailure;

  /**
   * Fornece o registro de logins.
   *
   * @return o registro de logins.
   */
  LoginRegistry loginRegistry();

  /**
   * Fornece o registro de ofertas.
   *
   * @return o registro de ofertas.
   */
  OfferRegistry offerRegistry();

  /**
   * Cria uma nova cadeia de chamadas para a entidade especificada, onde o dono
   * da cadeia � esta conex�o e utiliza-se a cadeia atual
   * ({@link OpenBusContext#joinedChain()}) como a cadeia que se deseja dar
   * seguimento ao encadeamento. � permitido especificar qualquer nome de
   * entidade, tendo ela um login ativo no momento ou n�o. A cadeia
   * resultante s� poder� ser utilizada com sucesso por uma conex�o que
   * possua a mesma identidade da entidade especificada.
   *
   * @param entity Nome da entidade para a qual deseja-se enviar a cadeia.
   * @return A cadeia gerada para ser utilizada pela entidade com o login
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
   * A cadeia criada somente pode ser utilizada pela entidade do login que faz a
   * chamada. O conte�do da cadeia � dado pelas informa��es obtidas atrav�s do
   * <i>token</i> indicado.
   *
   * @param token Valor opaco que representa uma informa��o de autentica��o
   *        externa.
   * @param domain Identificador do dom�nio de autentica��o.
   * @return A nova cadeia de chamadas assinada.
   *
   * @throws InvalidToken O token fornecido n�o foi reconhecido.
   * @throws UnknownDomain O dom�nio de autentica��o n�o � conhecido.
   * @throws WrongEncoding A importa��o falhou, pois o token n�o foi
   *            codificado corretamente com a chave p�blica do barramento.
   * @throws ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a cria��o da cadeia.
   */
  CallerChain importChain(byte[] token, String domain) throws InvalidToken,
    UnknownDomain, ServiceFailure, WrongEncoding;
}