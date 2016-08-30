package tecgraf.openbus;

import java.security.interfaces.RSAPrivateKey;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import org.omg.PortableServer.POA;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.*;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.WrongBus;

/**
 * Objeto que representa uma forma de acesso a um barramento.
 * <p>
 * Uma conex�o representa uma forma de acesso a um barramento. Basicamente, uma
 * conex�o � usada para representar uma identidade de acesso a um barramento. �
 * poss�vel uma aplica��o assumir m�ltiplas identidades ao acessar um ou mais
 * barramentos criando m�ltiplas conex�es para esses barramentos.
 * <p>
 * Para que as conex�es possam ser efetivamente utilizadas elas precisam estar
 * autenticadas no barramento, que pode ser visto como um identificador de
 * acesso. Cada login possui um identificador �nico e � autenticado em nome de
 * uma entidade, que pode representar um sistema computacional ou mesmo uma
 * pessoa. A fun��o da entidade � atribuir a responsabilidade �s chamadas feitas
 * com aquele login.
 * <p>
 * � importante notar que a conex�o s� define uma forma de acesso, mas n�o �
 * usada diretamente pela aplica��o ao realizar ou receber chamadas, pois as
 * chamadas ocorrem usando proxies e servants de um {@link ORB}. As conex�es que
 * s�o efetivamente usadas nas chamadas do ORB s�o definidas atrav�s do
 * {@link OpenBusContext} associado ao ORB.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Recupera o {@link ORB} correspondente ao {@link OpenBusContext} a partir do
   * qual essa conex�o foi criada.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Recupera o POA associado a essa conex�o.
   *
   * @return o POA
   */
  POA poa();

  /**
   * Recupera contexto do OpenBus associado a essa conex�o.
   *
   * @return o contexto do OpenBus
   */
  OpenBusContext context();

  /**
   * Recupera o identificador do barramento ao qual essa conex�o se refere.
   * 
   * @return o identificador do barramento.
   */
  String busid();

  /**
   * Recupera as Informa��es do login dessa conex�o ou <code>null</code> se a
   * conex�o n�o est� autenticada, ou seja, n�o tem um login v�lido no
   * barramento.
   * 
   * @return as informa��es do login.
   */
  LoginInfo login();

  /**
   * Efetua login de uma entidade usando autentica��o por senha.
   * <p>
   * A autentica��o por senha � validada usando um dos validadores de senha
   * definidos pelo adminsitrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autentica��o no barramento da entidade.
   * @param domain Identificador do dom�nio de autentica��o.
   * 
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception AccessDenied Senha fornecida para autentica��o da entidade n�o
   *            foi validada pelo barramento.
   * @exception TooManyAttempts A autentica��o foi recusada por um n�mero
   *            excessivo de tentativas inv�lidas de autentica��o por senha.
   * @exception UnknownDomain O dom�nio de autentica��o n�o � conhecido.
   * @exception WrongEncoding A autentica��o falhou, pois a senha n�o foi
   *            codificado corretamente com a chave p�blica do barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginByPassword(String entity, byte[] password, String domain)
    throws AccessDenied, AlreadyLoggedIn, TooManyAttempts, UnknownDomain,
    WrongEncoding, ServiceFailure;

  /**
   * Efetua login de uma entidade usando autentica��o por chave privada.
   * <p>
   * A autentica��o por chave privada � validada usando um certificado de login
   * registrado pelo adminsitrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey Chave privada correspondente ao certificado registrado a
   *        ser utilizada na autentica��o.
   * 
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception AccessDenied A chave privada fornecida n�o corresponde ao
   *            certificado da entidade registrado no barramento indicado.
   * @exception MissingCertificate N�o h� certificado para essa entidade
   *            registrado no barramento indicado.
   * @exception WrongEncoding A autentica��o falhou, pois a resposta ao desafio
   *            n�o foi codificada corretamente com a chave p�blica do
   *            barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginByPrivateKey(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedIn, AccessDenied, MissingCertificate, WrongEncoding,
    ServiceFailure;

  /**
   * Efetua login de uma entidade utilizando uma callback que retorne dados de
   * autentica��o, como autentica��es compartilhadas.
   * <p>
   * No caso espec�fico da autentica��o compartilhada, esta deve ser feita a
   * partir de um segredo obtido atrav�s da opera��o
   * {@link #startSharedAuth() startSharedAuth} de uma conex�o autenticada.
   *
   * @param callback Callback a ser utilizada para obter os dados de
   *                 autentica��o a serem utilizados no login.
   *
   * @exception InvalidLoginProcess A tentativa de login associada ao segredo
   *            informado � inv�lido, por exemplo depois do segredo ser
   *            cancelado, ter expirado, ou j� ter sido utilizado.
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception WrongBus As informa��es de autentica��o n�o pertencem ao
   *            barramento contactado.
   * @exception AccessDenied As informa��es de autentica��o n�o correspondem
   *            ao esperado pelo barramento.
   * @exception WrongEncoding A autentica��o falhou, pois a resposta ao desafio
   *            n�o foi codificada corretamente com a chave p�blica do
   *            barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginByCallback(LoginCallback callback) throws AlreadyLoggedIn, WrongBus,
    InvalidLoginProcess, AccessDenied, TooManyAttempts, UnknownDomain,
    WrongEncoding, MissingCertificate, ServiceFailure;

  /**
   * Inicia o processo de login por autentica��o compartilhada.
   * <p>
   * A autentica��o compartilhada permite criar um novo login compartilhando a
   * mesma autentica��o do login atual da conex�o. Portanto essa opera��o s�
   * pode ser chamada enquanto a conex�o estiver autenticada, caso contr�rio a
   * exce��o de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] � lan�ada. As
   * informa��es fornecidas por essa opera��o devem ser passadas para a opera��o
   * {@link #loginByCallback(LoginCallback)} para conclus�o do processo de
   * login por autentica��o compartilhada. Isso deve ser feito dentro do
   * tempo de lease definido pelo administrador do barramento. Caso contr�rio
   * essas informa��es se tornam inv�lidas e n�o podem mais ser utilizadas
   * para criar um login.
   * 
   * @return Segredo a ser fornecido na conclus�o do processo de login.
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a obten��o do objeto de login e segredo.
   */
  SharedAuthSecret startSharedAuth() throws ServiceFailure;

  /**
   * Efetua logout da conex�o, tornando o login atual inv�lido.
   * <p>
   * Ap�s a chamada a essa opera��o a conex�o fica desautenticada, implicando
   * que qualquer chamada realizada pelo ORB usando essa conex�o resultar� numa
   * exce��o de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] e chamadas
   * recebidas por esse ORB ser�o respondidas com a exce��o
   * {@link NO_PERMISSION}[{@link UnknownBusCode}] indicando que n�o foi
   * poss�vel validar a chamada pois a conex�o est� temporariamente
   * desautenticada.
   * Um logout expl�cito incorre na remo��o de todas as ofertas e todos os tipos
   * de observadores mantidos pela biblioteca de acesso OpenBus, tanto
   * localmente como no barramento. Um login posterior n�o levar� ao
   * recadastro autom�tico desses recursos.
   *
   * @return <code>true</code> se o processo de logout for conclu�do com �xito e
   *         <code>false</code> se n�o for poss�vel invalidar o login atual.
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento durante chamada ao remota.
   */
  boolean logout() throws ServiceFailure;

  /**
   * Recupera o registro de logins.
   *
   * @return o registro de logins.
   */
  LoginRegistry loginRegistry();

  /**
   * Recupera o registro de ofertas.
   *
   * @return o registro de ofertas.
   */
  OfferRegistry offerRegistry();

  /**
   * Cria uma cadeia de chamadas para a entidade especificada.
   * <p>
   * Cria uma nova cadeia de chamadas para a entidade especificada, onde o dono
   * da cadeia � esta conex�o e utiliza-se a cadeia atual
   * ({@link OpenBusContext#getJoinedChain()}) como a cadeia que se deseja dar
   * seguimento
   * ao encadeamento. � permitido especificar qualquer nome de entidade,
   * tendo ela um login ativo no momento ou n�o. A cadeia resultante s�
   * poder� ser utilizada ({@link OpenBusContext#joinChain(CallerChain)}) com sucesso por
   * uma conex�o que possua a mesma identidade da entidade especificada.
   *
   * @param entity nome da entidade para a qual deseja-se enviar a cadeia.
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
}