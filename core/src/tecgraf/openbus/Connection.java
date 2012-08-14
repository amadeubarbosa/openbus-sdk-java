package tecgraf.openbus;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.Current;

import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.BusChanged;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.InvalidPrivateKey;

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
 * {@link ConnectionManager} associado ao ORB.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Recupera o {@link ORB} correspondente ao {@link ConnectionManager} a partir
   * do qual essa conex�o foi criada.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Recupera a refer�ncia do servi�o n�cleo de registro de ofertas do
   * barramento ao qual a conex�o se refere.
   * 
   * @return o servi�o de registro de ofertas.
   */
  OfferRegistry offers();

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
   * 
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conex�o
   *            deve ser criada.
   * @exception AccessDenied Senha fornecida para autentica��o da entidade n�o
   *            foi validada pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginByPassword(String entity, byte[] password) throws AccessDenied,
    AlreadyLoggedIn, BusChanged, ServiceFailure;

  /**
   * Efetua login de uma entidade usando autentica��o por certificado.
   * <p>
   * A autentica��o por certificado � validada usando um certificado de login
   * registrado pelo adminsitrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey Chave privada correspondente ao certificado registrado a
   *        ser utilizada na autentica��o.
   * 
   * @exception InvalidPrivateKey A chave privada fornecida n�o � v�lida.
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conex�o
   *            deve ser criada.
   * @exception AccessDenied A chave privada fornecida n�o corresponde ao
   *            certificado da entidade registrado no barramento indicado.
   * @exception MissingCertificate N�o h� certificado para essa entidade
   *            registrado no barramento indicado.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginByCertificate(String entity, byte[] privateKey)
    throws InvalidPrivateKey, AlreadyLoggedIn, BusChanged, AccessDenied,
    MissingCertificate, ServiceFailure;

  /**
   * Inicia o processo de login por autentica��o compartilhada.
   * <p>
   * A autentica��o compartilhada permite criar um novo login compartilhando a
   * mesma autentica��o do login atual da conex�o. Portanto essa opera��o s�
   * pode ser chamada enquanto a conex�o estiver autenticada, caso contr�rio a
   * exce��o de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] � lan�ada. As
   * informa��es fornecidas por essa opera��o devem ser passadas para a opera��o
   * {@link #loginBySharedAuth(LoginProcess, byte[]) loginBySharedAuth} para
   * conclus�o do processo de login por autentica��o compartilhada. Isso deve
   * ser feito dentro do tempo de lease definido pelo administrador do
   * barramento. Caso contr�rio essas informa��es se tornam inv�lidas e n�o
   * podem mais ser utilizadas para criar um login.
   * 
   * @param secret Segredo a ser fornecido na conclus�o do processo de login.
   * 
   * @return Objeto que represeta o processo de login iniciado.
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a obten��o do objeto de login e segredo.
   */
  LoginProcess startSharedAuth(OctetSeqHolder secret) throws ServiceFailure;

  /**
   * Efetua login de uma entidade usando autentica��o compartilhada.
   * <p>
   * A autentica��o compartilhada � feita a partir de informa��es obtidas a
   * atrav�s da opera��o {@link #startSharedAuth(OctetSeqHolder)
   * startSharedAuth} de uma conex�o autenticada.
   * 
   * @param process Objeto que represeta o processo de login iniciado.
   * @param secret Segredo a ser fornecido na conclus�o do processo de login.
   * 
   * @exception InvalidLoginProcess O LoginProcess informado � inv�lido, por
   *            exemplo depois de ser cancelado ou ter expirado.
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conex�o
   *            deve ser criada.
   * @exception AccessDenied O segredo fornecido n�o corresponde ao esperado
   *            pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginBySharedAuth(LoginProcess process, byte[] secret)
    throws AlreadyLoggedIn, InvalidLoginProcess, BusChanged, AccessDenied,
    ServiceFailure;

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
   * 
   * @return <code>true</code> se o processo de logout for conclu�do com �xito e
   *         <code>false</code> se a conex�o j� estiver desautenticada (login
   *         inv�lido).
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento durante chamada ao remota.
   */
  boolean logout() throws ServiceFailure;

  /**
   * Callback a ser chamada quando o login atual se tornar inv�lido.
   * <p>
   * Esse atributo � utilizado para definir um objeto que implementa uma
   * interface de callback a ser chamada sempre que a conex�o receber uma
   * notifica��o de que o seu login est� inv�lido. Essas notifica��es ocorrem
   * durante chamadas realizadas ou recebidas pelo barramento usando essa
   * conex�o. Um login pode se tornar inv�lido caso o administrador
   * explicitamente o torne inv�lido ou caso a thread interna de renova��o de
   * login n�o seja capaz de renovar o lease do login a tempo. Caso esse
   * atributo seja <code>null</code>, nenhum objeto de callback � chamado na
   * ocorr�ncia desse evento.
   * <p>
   * Durante a execu��o dessa callback um novo login pode ser restabelecido.
   * Neste caso, a chamada do barramento que recebeu a notifica��o de login
   * inv�lido � refeita usando o novo login, caso contr�rio, a chamada original
   * lan�a a exce��o de de sistema {@link NO_PERMISSION}[{@link NoLoginCode}].
   * 
   * @param callback Objeto que implementa a interface de callback a ser chamada
   *        ou <code>null</code> caso nenhum objeto deva ser chamado na
   *        ocorr�ncia desse evento.
   */
  void onInvalidLoginCallback(InvalidLoginCallback callback);

  /**
   * Recupera a callback a ser chamada sempre que o login se torna inv�lido.
   * 
   * @return a callback ou <code>null</code> caso ela n�o exista.
   */
  InvalidLoginCallback onInvalidLoginCallback();

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

}